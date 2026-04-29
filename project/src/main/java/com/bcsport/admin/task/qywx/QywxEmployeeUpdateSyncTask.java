package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeModificationMapper;
import com.bcsport.admin.qywxmapper.QywxAttrsBaseMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 更新企微员工信息任务
 *
 * 从 IHR employee_modifications 表获取每日调整的员工 ID，
 * 查询员工最新详细信息后调用企微 API 更新通讯录成员。
 */
@Slf4j
@Component("qywxEmployeeUpdateSyncTask")
public class QywxEmployeeUpdateSyncTask {

    private static final String DEFAULT_DEPART_ID = "1997";
    private static final int BATCH_QUERY_SIZE = 100;

    @Autowired
    private IhrEmployeeModificationMapper modificationMapper;

    @Autowired
    private IhrEmployeeDetailMapper employeeDetailMapper;

    @Autowired
    private QywxDepartmentMapper departmentMapper;

    @Autowired
    private QywxAttrsBaseMapper attrsBaseMapper;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private IhrEmployeeExclusionService exclusionService;

    public void sync() {
        log.info("========================================");
        log.info("=== 开始执行：更新企微员工信息 ===");
        log.info("========================================");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取今日和昨日的调整员工 ID
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = cal.getTime();

            List<Date> dates = Arrays.asList(yesterday, today);
            List<String> modifiedEmployeeIds = modificationMapper.selectBySyncDates(dates);

            if (modifiedEmployeeIds == null || modifiedEmployeeIds.isEmpty()) {
                log.info("没有需要更新的员工，跳过");
                log.info("=== 更新企微员工信息 完成，无数据 ===");
                return;
            }

            log.info("获取到 {} 个调整员工 ID", modifiedEmployeeIds.size());

            // 2. 分批查询员工详细信息
            List<IhrEmployeeDetail> allEmployees = new ArrayList<>();
            for (int i = 0; i < modifiedEmployeeIds.size(); i += BATCH_QUERY_SIZE) {
                int end = Math.min(i + BATCH_QUERY_SIZE, modifiedEmployeeIds.size());
                List<String> batchIds = modifiedEmployeeIds.subList(i, end);
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

                    // 检查是否在排除列表中
                    if (exclusionService.checkExcluded(staffName, staffNo, 1)) {
                        log.info("员工 {}({}) 在排除列表中，跳过", staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 通过工号查企微 userid
                    String userid = attrsBaseMapper.selectUseridByStaffNo(staffNo);
                    if (userid == null || userid.isEmpty()) {
                        // 兜底：通过手机号查
                        userid = apiClient.getUserIdByMobile(mobile);
                    }
                    if (userid == null || userid.isEmpty()) {
                        log.warn("员工 {}({}) 未找到企微userid，跳过", staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 匹配部门 ID
                    String departId = resolveDepartId(employee);

                    // 构建更新请求体
                    JSONObject requestBody = buildUpdateUserBody(userid, employee, departId);

                    // 调用更新 API
                    JSONObject result = apiClient.updateUser(requestBody);
                    Integer errcode = result.getInt("errcode");
                    if (errcode != null && errcode == 0) {
                        successCount++;
                        log.info("更新企微用户成功: {}({}) -> userid: {}", staffName, mobile, userid);
                    } else {
                        failCount++;
                        log.warn("更新企微用户失败: {}({}), errcode: {}, errmsg: {}",
                                staffName, mobile, errcode, result.getStr("errmsg"));
                    }

                } catch (Exception e) {
                    failCount++;
                    log.error("处理员工失败: {}({}): {}", employee.getStaffName(), employee.getMobileNo(), e.getMessage());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("========================================");
            log.info("=== 更新企微员工信息 完成 ===");
            log.info("=== 成功: {}, 跳过: {}, 失败: {}, 总耗时: {} ms ===", successCount, skipCount, failCount, totalTime);
            log.info("========================================");

        } catch (Exception e) {
            log.error("=== 更新企微员工信息 异常 ===", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 匹配企微部门 ID
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

            if (departIds.size() == 1) {
                return departIds.get(0);
            }

            // 多个匹配，用一级部门名进一步筛选
            String firstLevelDeptName = employee.getFirstLevelDepartmentName();
            if (firstLevelDeptName == null || firstLevelDeptName.isEmpty()) {
                log.warn("部门 {} 有多个匹配但无一级部门信息, 使用第一个: {}", departmentName, departIds.get(0));
                return departIds.get(0);
            }

            // 主管岗位需三级部门匹配
            String positionName = employee.getPositionName();
            if (positionName != null && positionName.contains("主管")) {
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
     * 构建更新企微用户的请求体
     */
    private JSONObject buildUpdateUserBody(String userid, IhrEmployeeDetail employee, String departId) {
        JSONObject body = new JSONObject();
        body.set("userid", userid);

        if (employee.getStaffName() != null && !"null".equals(employee.getStaffName())) {
            body.set("name", employee.getStaffName());
        }

        if (employee.getMobileNo() != null && !"null".equals(employee.getMobileNo())) {
            body.set("mobile", employee.getMobileNo());
        }

        if (employee.getPositionName() != null && !"null".equals(employee.getPositionName())) {
            body.set("position", employee.getPositionName());
        }

        if (employee.getWorkEmail() != null && !"null".equals(employee.getWorkEmail()) && !employee.getWorkEmail().isEmpty()) {
            body.set("email", employee.getWorkEmail());
        }

        JSONArray deptArray = new JSONArray();
        deptArray.add(Integer.parseInt(departId));
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
