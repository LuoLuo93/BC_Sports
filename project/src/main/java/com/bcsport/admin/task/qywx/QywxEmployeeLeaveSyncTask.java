package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.qywxmapper.QywxAttrsBaseMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberDetailMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import com.bcsport.admin.service.IhrEmployeeLeavingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 企微员工离职同步任务
 *
 * 从 IHR employee_information 表查询最近离职的员工，
 * 通过姓名 + 工号双重校验后在企微通讯录中删除。
 */
@Slf4j
@Component("qywxEmployeeLeaveSyncTask")
public class QywxEmployeeLeaveSyncTask {

    @Autowired
    private IhrEmployeeDetailMapper employeeDetailMapper;

    @Autowired
    private QywxAttrsBaseMapper attrsBaseMapper;

    @Autowired
    private QywxDepartmentMemberDetailMapper memberDetailMapper;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private IhrEmployeeExclusionService exclusionService;

    @Autowired
    private IhrEmployeeLeavingService leavingService;

    public void sync() {
        log.info("=== 开始执行: 企微员工离职同步 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 查询最近离职的员工（leaveDate 在前天~今天范围内，覆盖周末/假期）
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();

            cal.add(Calendar.DAY_OF_MONTH, -3);
            Date startDate = cal.getTime();

            List<IhrEmployeeDetail> leavedEmployees = employeeDetailMapper.selectRecentLeaved(startDate, today);

            if (leavedEmployees == null || leavedEmployees.isEmpty()) {
                log.info("没有离职员工，跳过");
                log.info("=== 企微员工离职同步 完成，无数据 ===");
                return;
            }

            log.info("查到 {} 个离职员工，开始处理", leavedEmployees.size());

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (IhrEmployeeDetail employee : leavedEmployees) {
                String employeeId = employee.getId();
                String staffName = employee.getStaffName();
                String staffNo = employee.getStaffNo();

                try {
                    // 检查是否在离职排除列表中
                    if (exclusionService.checkExcluded(staffName, staffNo, 2)) {
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    if (staffNo == null || staffNo.isEmpty() || "null".equals(staffNo)) {
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 1. 通过工号查找企微 userid
                    String qywxUserid = attrsBaseMapper.selectUseridByStaffNo(staffNo);
                    if (qywxUserid == null || qywxUserid.isEmpty()) {
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 2. 通过 userid 查企微中的姓名，与 IHR 姓名比对
                    String qywxName = memberDetailMapper.selectNameByUserid(qywxUserid);
                    if (qywxName == null || !qywxName.equals(staffName)) {
                        log.warn("姓名不匹配，跳过: 工号={}, IHR姓名={}, 企微姓名={}, userid={}",
                                staffNo, staffName, qywxName, qywxUserid);
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // 3. 姓名和工号都匹配，执行删除
                    JSONObject result = apiClient.deleteUser(qywxUserid);
                    Integer errcode = result.getInt("errcode");
                    if (errcode != null && errcode == 0) {
                        successCount++;
                        leavingService.markSyncSuccess(employeeId, staffName, staffNo);
                    } else {
                        // 60111/46004 表示用户不存在，不算失败
                        if (errcode != null && (errcode == 60111 || errcode == 46004)) {
                            skipCount++;
                            leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        } else {
                            failCount++;
                            String errmsg = result.getStr("errmsg");
                            leavingService.markSyncFailed(employeeId, staffName, staffNo,
                                    "errcode=" + errcode + ", " + errmsg);
                            log.warn("删除企微用户失败: {}(工号={}), errcode: {}, errmsg: {}",
                                    staffName, staffNo, errcode, errmsg);
                        }
                    }

                } catch (Exception e) {
                    failCount++;
                    leavingService.markSyncFailed(employeeId, staffName, staffNo, e.getMessage());
                    log.error("处理离职员工失败: {}({}): {}", staffName, staffNo, e.getMessage());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== 完成: 企微员工离职同步, 成功: {}, 跳过: {}, 失败: {}, 耗时: {} ms ===",
                    successCount, skipCount, failCount, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 企微员工离职同步 ===", e);
            
        }
    }

    /**
     * 同步单个离职员工到企微（供手动触发使用）
     */
    public String syncSingle(String employeeId) {
        log.info("手动同步单个离职员工到企微, employeeId={}", employeeId);

        IhrEmployeeDetail employee = employeeDetailMapper.selectById(employeeId);
        if (employee == null) {
            return "未找到员工详细信息";
        }

        String staffName = employee.getStaffName();
        String staffNo = employee.getStaffNo();

        if (exclusionService.checkExcluded(staffName, staffNo, 2)) {
            leavingService.markSyncSkipped(employeeId, staffName, staffNo);
            return "员工在排除列表中";
        }

        if (staffNo == null || staffNo.isEmpty() || "null".equals(staffNo)) {
            leavingService.markSyncSkipped(employeeId, staffName, staffNo);
            return "工号无效";
        }

        String qywxUserid = attrsBaseMapper.selectUseridByStaffNo(staffNo);
        if (qywxUserid == null || qywxUserid.isEmpty()) {
            leavingService.markSyncSkipped(employeeId, staffName, staffNo);
            return "未找到企微userid";
        }

        String qywxName = memberDetailMapper.selectNameByUserid(qywxUserid);
        if (qywxName == null || !qywxName.equals(staffName)) {
            leavingService.markSyncSkipped(employeeId, staffName, staffNo);
            return "姓名不匹配";
        }

        JSONObject result = apiClient.deleteUser(qywxUserid);
        Integer errcode = result.getInt("errcode");

        if (errcode != null && errcode == 0) {
            leavingService.markSyncSuccess(employeeId, staffName, staffNo);
            return null;
        } else if (errcode != null && (errcode == 60111 || errcode == 46004)) {
            leavingService.markSyncSkipped(employeeId, staffName, staffNo);
            return "用户已不存在";
        } else {
            String errmsg = result.getStr("errmsg");
            leavingService.markSyncFailed(employeeId, staffName, staffNo,
                    "errcode=" + errcode + ", " + errmsg);
            return "删除失败: " + errmsg;
        }
    }
}
