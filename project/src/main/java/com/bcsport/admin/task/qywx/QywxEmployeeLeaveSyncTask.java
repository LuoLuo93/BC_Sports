package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.qywxmapper.QywxAttrsBaseMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberDetailMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
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

    public void sync() {
        log.info("========================================");
        log.info("=== 开始执行：企微员工离职同步 ===");
        log.info("========================================");
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
                try {
                    String staffNo = employee.getStaffNo();
                    String staffName = employee.getStaffName();

                    // 检查是否在离职排除列表中
                    if (exclusionService.checkExcluded(staffName, staffNo, 2)) {
                        log.info("员工 {}({}) 在离职排除列表中，跳过", staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    if (staffNo == null || staffNo.isEmpty() || "null".equals(staffNo)) {
                        log.debug("跳过无效工号: {}", staffName);
                        skipCount++;
                        continue;
                    }

                    // 1. 通过工号查找企微 userid
                    String qywxUserid = attrsBaseMapper.selectUseridByStaffNo(staffNo);
                    if (qywxUserid == null || qywxUserid.isEmpty()) {
                        log.info("工号 {}({}) 在企微中未找到，跳过", staffNo, staffName);
                        skipCount++;
                        continue;
                    }

                    // 2. 通过 userid 查企微中的姓名，与 IHR 姓名比对
                    String qywxName = memberDetailMapper.selectNameByUserid(qywxUserid);
                    if (qywxName == null || !qywxName.equals(staffName)) {
                        log.warn("姓名不匹配，跳过: 工号={}, IHR姓名={}, 企微姓名={}, userid={}",
                                staffNo, staffName, qywxName, qywxUserid);
                        skipCount++;
                        continue;
                    }

                    // 3. 姓名和工号都匹配，执行删除
                    JSONObject result = apiClient.deleteUser(qywxUserid);
                    Integer errcode = result.getInt("errcode");
                    if (errcode != null && errcode == 0) {
                        successCount++;
                        log.info("删除企微用户成功: {}(工号={}, userid={})", staffName, staffNo, qywxUserid);
                    } else {
                        // 60111 表示用户不存在，不算失败
                        if (errcode != null && errcode == 60111) {
                            skipCount++;
                            log.info("用户已不存在(errcode=60111): {}(工号={})", staffName, staffNo);
                        } else {
                            failCount++;
                            log.warn("删除企微用户失败: {}(工号={}), errcode: {}, errmsg: {}",
                                    staffName, staffNo, errcode, result.getStr("errmsg"));
                        }
                    }

                } catch (Exception e) {
                    failCount++;
                    log.error("处理离职员工失败: {}({}): {}", employee.getStaffName(), employee.getStaffNo(), e.getMessage());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("========================================");
            log.info("=== 企微员工离职同步 完成 ===");
            log.info("=== 成功: {}, 跳过: {}, 失败: {}, 总耗时: {} ms ===", successCount, skipCount, failCount, totalTime);
            log.info("========================================");

        } catch (Exception e) {
            log.error("=== 企微员工离职同步 异常 ===", e);
            throw new RuntimeException(e);
        }
    }
}
