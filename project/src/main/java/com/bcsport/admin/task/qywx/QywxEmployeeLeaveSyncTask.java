package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.entity.ihr.IhrEmployeeExclusion;
import com.bcsport.admin.entity.qywx.QywxAttrsBase;
import com.bcsport.admin.entity.qywx.QywxDepartmentMemberDetail;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.qywxmapper.QywxAttrsBaseMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberDetailMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import com.bcsport.admin.service.IhrEmployeeLeavingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 企微员工离职同步任务
 *
 * 从 IHR employee_information 表按状态捞取所有未同步成功的离职员工
 * (从未同步/上次失败/同步早于离职日期/跳过满7天复检,不设日期窗口——
 *  IHR 补录离职常晚于官方离职日数天,日期窗口会导致永久漏删),
 * 通过姓名 + 工号双重校验后在企微通讯录中删除。
 */
@Slf4j
@Component("qywxEmployeeLeaveSyncTask")
public class QywxEmployeeLeaveSyncTask {

    /** IN 查询分片大小：SQL Server 单请求最多 2100 个参数,按状态全量捞取后工号/userid 可达数千 */
    private static final int IN_CHUNK_SIZE = 500;

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
    private IhrEmployeeExclusionMapper exclusionMapper;

    @Autowired
    private IhrEmployeeLeavingService leavingService;

    public void sync() {
        log.info("=== 开始执行: 企微员工离职同步 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 按状态捞取所有未同步成功的离职者(从未同步/失败/过期/跳过满7天复检),不限日期。
            // 旧版 leave_date 近 3 天窗口在 IHR 补录晚到时永久漏人(2026-08-31 批次 16 人实证),
            // 状态口径与离职管理页 effectiveSyncStatus 一致,补录多晚都会被下一班兜住。
            List<IhrEmployeeDetail> leavedEmployees = employeeDetailMapper.selectUnsyncedLeaved();

            if (leavedEmployees == null || leavedEmployees.isEmpty()) {
                log.info("没有离职员工，跳过");
                log.info("=== 企微员工离职同步 完成，无数据 ===");
                return;
            }

            log.info("查到 {} 个离职员工，开始处理", leavedEmployees.size());

            // Pre-load exclusions for O(1) lookup
            List<IhrEmployeeExclusion> exclusions = exclusionMapper.selectActiveExclusions(2);
            Set<String> exclusionSet = exclusions.stream()
                    .map(e -> e.getStaffName() + "|" + e.getStaffNo())
                    .collect(Collectors.toSet());

            // Pre-load userid-by-staffNo mappings
            List<String> allStaffNos = leavedEmployees.stream()
                    .map(IhrEmployeeDetail::getStaffNo)
                    .filter(sn -> sn != null && !sn.isEmpty() && !"null".equals(sn))
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, String> staffNoToUserid = new HashMap<>();
            if (!allStaffNos.isEmpty()) {
                for (int i = 0; i < allStaffNos.size(); i += IN_CHUNK_SIZE) {
                    List<String> chunk = allStaffNos.subList(i, Math.min(i + IN_CHUNK_SIZE, allStaffNos.size()));
                    List<QywxAttrsBase> attrsList = attrsBaseMapper.selectUseridByStaffNos(chunk);
                    for (QywxAttrsBase attr : attrsList) {
                        if (attr.getEnrollInDate() != null && attr.getUserid() != null) {
                            staffNoToUserid.put(attr.getEnrollInDate(), attr.getUserid());
                        }
                    }
                }
            }

            // Pre-load name-by-userid mappings
            List<String> userids = new ArrayList<>(staffNoToUserid.values());
            Map<String, String> useridToName = new HashMap<>();
            if (!userids.isEmpty()) {
                for (int i = 0; i < userids.size(); i += IN_CHUNK_SIZE) {
                    List<String> chunk = userids.subList(i, Math.min(i + IN_CHUNK_SIZE, userids.size()));
                    List<QywxDepartmentMemberDetail> members = memberDetailMapper.selectNameByUserids(chunk);
                    for (QywxDepartmentMemberDetail m : members) {
                        if (m.getUserid() != null && m.getName() != null) {
                            useridToName.put(m.getUserid(), m.getName());
                        }
                    }
                }
            }

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (IhrEmployeeDetail employee : leavedEmployees) {
                String employeeId = employee.getId();
                String staffName = employee.getStaffName();
                String staffNo = employee.getStaffNo();

                try {
                    // Check exclusion using pre-loaded Set
                    if (exclusionSet.contains(staffName + "|" + staffNo)) {
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    if (staffNo == null || staffNo.isEmpty() || "null".equals(staffNo)) {
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // Look up userid from pre-loaded Map
                    String qywxUserid = staffNoToUserid.get(staffNo);
                    if (qywxUserid == null || qywxUserid.isEmpty()) {
                        leavingService.markSyncSkipped(employeeId, staffName, staffNo);
                        skipCount++;
                        continue;
                    }

                    // Look up name from pre-loaded Map
                    String qywxName = useridToName.get(qywxUserid);
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
            throw e;
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
