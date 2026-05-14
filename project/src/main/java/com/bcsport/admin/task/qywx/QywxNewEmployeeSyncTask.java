package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.ihrmapper.IhrEmployeeAdditionMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import com.bcsport.admin.service.IhrEmployeeOnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 录入企微新员工任务
 *
 * 从 IHR employee_additions 表获取每日新入职员工 ID，
 * 查询员工详细信息后调用企微 API 创建通讯录成员。
 */
@Slf4j
@Component("qywxNewEmployeeSyncTask")
public class QywxNewEmployeeSyncTask {

    private static final String DEFAULT_DEPART_ID = "1997";
    private static final int BATCH_QUERY_SIZE = 100;

    @Autowired
    private IhrEmployeeAdditionMapper additionMapper;

    @Autowired
    private IhrEmployeeDetailMapper employeeDetailMapper;

    @Autowired
    private QywxDepartmentMapper departmentMapper;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxDepartmentTask departmentTask;

    @Autowired
    private IhrEmployeeExclusionService exclusionService;

    @Autowired
    private IhrEmployeeOnboardingService onboardingService;

    public void sync() {
        log.info("=== 开始执行: 录入企微新员工 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 0. 先同步企微部门，确保部门数据最新
            log.info("--- 步骤 0: 同步企微部门 ---");
            try {
                departmentTask.sync();
            } catch (Exception e) {
                log.warn("同步部门失败，将使用现有部门数据: {}", e.getMessage());
            }


            // 1. 获取今日和昨日的新入职员工 ID
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = cal.getTime();

            List<Date> dates = Arrays.asList(yesterday, today);
            List<String> newEmployeeIds = additionMapper.selectBySyncDates(dates);

            if (newEmployeeIds == null || newEmployeeIds.isEmpty()) {
                log.info("没有新入职员工，跳过");
                log.info("=== 录入企微新员工 完成，无数据 ===");
                return;
            }

            log.info("获取到 {} 个新入职员工 ID", newEmployeeIds.size());

            // 2. 分批查询员工详细信息
            List<IhrEmployeeDetail> allEmployees = new ArrayList<>();
            for (int i = 0; i < newEmployeeIds.size(); i += BATCH_QUERY_SIZE) {
                int end = Math.min(i + BATCH_QUERY_SIZE, newEmployeeIds.size());
                List<String> batchIds = newEmployeeIds.subList(i, end);
                try {
                    List<IhrEmployeeDetail> batch = employeeDetailMapper.selectByStaffIds(batchIds);
                    if (batch != null) {
                        allEmployees.addAll(batch);
                    }
                } catch (Exception e) {
                    log.error("查询员工详情失败, 批次 {}-{}: {}", i + 1, end, e.getMessage());
                }
            }

            if (allEmployees.isEmpty()) {
                log.info("未查到员工详细信息，跳过");
                return;
            }

            log.info("查到 {} 个员工详细信息，开始处理", allEmployees.size());

            // 3. 逐个处理员工
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (IhrEmployeeDetail employee : allEmployees) {
                try {
                    String staffName = employee.getStaffName();
                    String staffNo = employee.getStaffNo();
                    String mobile = employee.getMobileNo();

                    // 手机号为空则跳过
                    if (mobile == null || mobile.isEmpty() || "null".equals(mobile)) {
                        log.warn("员工 {}({}) 无手机号，跳过", staffName, staffNo);
                        onboardingService.markSyncSkipped(employee.getId(), staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 检查是否在入职排除列表中
                    if (exclusionService.checkExcluded(staffName, staffNo, 1)) {
                        log.info("员工 {}({}) 在入职排除列表中，跳过", staffName, staffNo);
                        onboardingService.markSyncSkipped(employee.getId(), staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 检查企微是否已存在
                    String existingUserId = apiClient.getUserIdByMobile(mobile);
                    if (existingUserId != null) {
                        log.info("员工 {}({}) 已在企微存在，跳过", employee.getStaffName(), mobile);
                        onboardingService.markSyncSkipped(employee.getId(), staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 匹配部门 ID
                    String departId = resolveDepartId(employee);
                    log.info("员工 {}({}), 部门 {} -> 企微部门ID: {}", employee.getStaffName(), mobile, employee.getDepartmentName(), departId);

                    // 构建创建用户请求
                    JSONObject requestBody = buildCreateUserBody(employee, departId);

                    // 调用创建 API
                    JSONObject result = apiClient.createUser(requestBody);
                    Integer errcode = result.getInt("errcode");
                    if (errcode != null && errcode == 0) {
                        successCount++;
                        onboardingService.markSyncSuccess(employee.getId(), staffName, staffNo);
                        log.info("创建企微用户成功: {}({})", employee.getStaffName(), mobile);
                    } else {
                        // 60102 表示已存在，不算失败
                        if (errcode != null && errcode == 60102) {
                            skipCount++;
                            onboardingService.markSyncSkipped(employee.getId(), staffName, staffNo);
                            log.info("员工 {}({}) 创建时发现已存在(errcode=60102)，跳过", employee.getStaffName(), mobile);
                        } else {
                            failCount++;
                            String errmsg = result.getStr("errmsg");
                            onboardingService.markSyncFailed(employee.getId(), staffName, staffNo,
                                    "errcode=" + errcode + ", " + errmsg);
                            log.warn("创建企微用户失败: {}({}), errcode: {}, errmsg: {}",
                                    employee.getStaffName(), mobile, errcode, errmsg);
                        }
                    }

                } catch (Exception e) {
                    failCount++;
                    onboardingService.markSyncFailed(employee.getId(), employee.getStaffName(), employee.getStaffNo(), e.getMessage());
                    log.error("处理员工失败: {}({}): {}", employee.getStaffName(), employee.getMobileNo(), e.getMessage());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== 完成: 录入企微新员工, 成功: {}, 跳过: {}, 失败: {}, 耗时: {} ms ===",
                    successCount, skipCount, failCount, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 录入企微新员工 ===", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步单个入职员工到企微（供手动触发使用）
     */
    public String syncSingle(String employeesId) {
        log.info("手动同步单个员工到企微, employeesId={}", employeesId);

        List<IhrEmployeeDetail> details = employeeDetailMapper.selectByStaffIds(Collections.singletonList(employeesId));
        if (details == null || details.isEmpty()) {
            return "未找到员工详细信息";
        }

        IhrEmployeeDetail employee = details.get(0);
        String staffName = employee.getStaffName();
        String staffNo = employee.getStaffNo();
        String mobile = employee.getMobileNo();

        if (mobile == null || mobile.isEmpty() || "null".equals(mobile)) {
            return "员工无手机号";
        }

        // 检查排除列表
        if (exclusionService.checkExcluded(staffName, staffNo, 1)) {
            onboardingService.markSyncSkipped(employeesId, staffName, staffNo);
            return "员工在入职排除列表中";
        }

        // 检查企微是否已存在
        String existingUserId = apiClient.getUserIdByMobile(mobile);
        if (existingUserId != null) {
            onboardingService.markSyncSkipped(employeesId, staffName, staffNo);
            return "员工已在企微存在";
        }

        // 匹配部门
        String departId = resolveDepartId(employee);

        // 创建企微用户
        JSONObject requestBody = buildCreateUserBody(employee, departId);
        JSONObject result = apiClient.createUser(requestBody);
        Integer errcode = result.getInt("errcode");

        if (errcode != null && errcode == 0) {
            onboardingService.markSyncSuccess(employeesId, staffName, staffNo);
            return null;
        } else if (errcode != null && errcode == 60102) {
            onboardingService.markSyncSkipped(employeesId, staffName, staffNo);
            return "员工已在企微存在(errcode=60102)";
        } else {
            String errmsg = result.getStr("errmsg");
            onboardingService.markSyncFailed(employeesId, staffName, staffNo,
                    "errcode=" + errcode + ", " + errmsg);
            return "创建失败: " + errmsg;
        }
    }

    /**
     * 匹配企微部门 ID（复用原项目多级匹配逻辑）
     */
    private String resolveDepartId(IhrEmployeeDetail employee) {
        String departmentName = employee.getDepartmentName();
        if (departmentName == null || departmentName.isEmpty()) {
            return DEFAULT_DEPART_ID;
        }

        try {
            // 第一级：直接按部门名查
            List<String> departIds = departmentMapper.selectDepartIdByName(departmentName);
            if (departIds == null || departIds.isEmpty()) {
                log.warn("未找到部门: {}, 分配到默认部门", departmentName);
                return DEFAULT_DEPART_ID;
            }

            // 只有一个匹配，直接返回
            if (departIds.size() == 1) {
                return departIds.get(0);
            }

            // 多个匹配，需要用一级部门名进一步筛选
            String firstLevelDeptName = employee.getFirstLevelDepartmentName();
            if (firstLevelDeptName == null || firstLevelDeptName.isEmpty()) {
                log.warn("部门 {} 有多个匹配但无一级部门信息, 使用第一个: {}", departmentName, departIds.get(0));
                return departIds.get(0);
            }

            // 判断是否主管岗位（需要三级部门匹配）
            String positionName = employee.getPositionName();
            if (positionName != null && positionName.contains("主管")) {
                // 三级部门匹配：部门名 -> 上级部门 -> 一级部门
                // 先查当前部门的上级部门名（通过 department 表的 parentId 关系）
                String result = departmentMapper.selectDepartIdByThreeLevel(
                        departmentName, firstLevelDeptName, firstLevelDeptName);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }

            // 二级匹配：部门名 + 一级部门名
            String result = departmentMapper.selectDepartIdByNameAndParent(departmentName, firstLevelDeptName);
            if (result != null && !result.isEmpty()) {
                return result;
            }

            log.warn("未找到部门 {} 的精确匹配(一级部门: {}), 分配到默认部门", departmentName, firstLevelDeptName);
            return DEFAULT_DEPART_ID;
        } catch (Exception e) {
            log.error("匹配部门失败: {}: {}", departmentName, e.getMessage());
            return DEFAULT_DEPART_ID;
        }
    }

    /**
     * 构建创建企微用户的请求体
     */
    private JSONObject buildCreateUserBody(IhrEmployeeDetail employee, String departId) {
        JSONObject body = new JSONObject();
        body.set("userid", employee.getMobileNo());
        body.set("name", employee.getStaffName());
        body.set("mobile", employee.getMobileNo());

        if (employee.getPositionName() != null && !"null".equals(employee.getPositionName())) {
            body.set("position", employee.getPositionName());
        }

        // 邮箱
        if (employee.getWorkEmail() != null && !"null".equals(employee.getWorkEmail()) && !employee.getWorkEmail().isEmpty()) {
            body.set("email", employee.getWorkEmail());
        }

        JSONArray deptArray = new JSONArray();
        try {
            deptArray.add(Integer.parseInt(departId));
        } catch (NumberFormatException e) {
            deptArray.add(1);
        }
        body.set("department", deptArray);

        // 扩展属性
        JSONArray attrs = new JSONArray();

        if (employee.getStaffNo() != null && !"null".equals(employee.getStaffNo())) {
            attrs.add(buildAttr("工号", employee.getStaffNo()));
        }
        if (employee.getPositionLevelName() != null && !"null".equals(employee.getPositionLevelName())) {
            attrs.add(buildAttr("职级", employee.getPositionLevelName()));
        }
        if (employee.getEnrollInDate() != null && !"null".equals(employee.getEnrollInDate())) {
            attrs.add(buildAttr("入职日期", employee.getEnrollInDate()));
        }
        if (employee.getMobileNo() != null && !"null".equals(employee.getMobileNo())) {
            attrs.add(buildAttr("mobile", employee.getMobileNo()));
        }

        if (!attrs.isEmpty()) {
            JSONObject extattr = new JSONObject();
            extattr.set("attrs", attrs);
            body.set("extattr", extattr);
        }

        return body;
    }

    private JSONObject buildAttr(String name, String value) {
        JSONObject attr = new JSONObject();
        attr.set("name", name);
        attr.set("type", 0);
        JSONObject text = new JSONObject();
        text.set("value", value);
        attr.set("text", text);
        attr.set("value", value);
        return attr;
    }
}
