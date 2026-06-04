package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.ihrmapper.ErpEmployeeSyncStatusMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.service.ErpEmployeeSyncService;
import com.bcsport.admin.vo.ErpEmployeeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ERP员工同步任务
 * <p>
 * 数据流: IHR员工数据 → 伯俊ERP(表14630)
 * <ul>
 *   <li>入职(ObjectCreate): 新增员工到ERP</li>
 *   <li>变更(ObjectModify): 增量更新ERP员工</li>
 *   <li>离职(ObjectModify): 更新ERP员工为离职状态</li>
 * </ul>
 */
@Slf4j
@Component("erpEmployeeSyncTask")
public class ErpEmployeeSyncTask {

    private static volatile boolean syncing = false;

    @Autowired
    private ErpEmployeeSyncService erpSyncService;

    @Autowired
    private ErpEmployeeSyncStatusMapper syncStatusMapper;

    @Autowired
    private IhrEmployeeDetailMapper employeeDetailMapper;

    @Autowired
    private BjErpApiClient bjErpApiClient;

    public static boolean isSyncing() {
        return syncing;
    }

    /**
     * 同步所有待同步人员到伯俊ERP
     * 数据源与前端Tab页一致：从 employee_additions / employee_modifications / employee_information 出发，
     * LEFT JOIN erp_employee_sync_status 判断有效同步状态
     */
    public void syncAll() {
        synchronized (ErpEmployeeSyncTask.class) {
            if (syncing) {
                log.warn("ERP人员同步正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: ERP人员同步到伯俊 ===");

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        try {
            // 1. 入职同步
            List<ErpEmployeeVO> onboardings = syncStatusMapper.selectPendingOnboardings();
            log.info("待同步入职人员: {}", onboardings.size());
            for (ErpEmployeeVO vo : onboardings) {
                int[] result = syncOne("ONBOARDING", vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo());
                successCount += result[0];
                failCount += result[1];
                skipCount += result[2];
            }

            // 2. 变更同步
            List<ErpEmployeeVO> updates = syncStatusMapper.selectPendingUpdates();
            log.info("待同步变更人员: {}", updates.size());
            for (ErpEmployeeVO vo : updates) {
                int[] result = syncOne("UPDATE", vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo());
                successCount += result[0];
                failCount += result[1];
                skipCount += result[2];
            }

            // 3. 离职同步（ERP离职接口暂未提供，先注释）
            // List<ErpEmployeeVO> leavings = syncStatusMapper.selectPendingLeavings();
            // log.info("待同步离职人员: {}", leavings.size());
            // for (ErpEmployeeVO vo : leavings) {
            //     int[] result = syncOne("LEAVING", vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo());
            //     successCount += result[0];
            //     failCount += result[1];
            //     skipCount += result[2];
            // }

        } finally {
            synchronized (ErpEmployeeSyncTask.class) {
                syncing = false;
            }
        }

        log.info("=== ERP人员同步完成: 成功={}, 失败={}, 跳过={}, 总计={} ===",
                successCount, failCount, skipCount, successCount + failCount + skipCount);
    }

    /**
     * 同步单个员工（syncAll和syncSingle共用）
     *
     * @return [successCount, failCount, skipCount]
     */
    private int[] syncOne(String syncType, String employeeId, String staffName, String staffNo) {
        try {
            IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
            if (detail == null) {
                log.warn("员工详情未找到, employeeId={}, staffName={}, 跳过", employeeId, staffName);
                erpSyncService.markSyncSkipped(syncType, employeeId, staffName, staffNo);
                return new int[]{0, 0, 1};
            }

            Long erpObjectId;
            switch (syncType) {
                case "ONBOARDING":
                    erpObjectId = syncOnboarding(detail);
                    break;
                case "UPDATE":
                    erpObjectId = syncUpdate(detail);
                    break;
                case "LEAVING":
                    erpObjectId = syncLeaving(detail);
                    break;
                default:
                    log.warn("未知同步类型: {}, employeeId={}, 跳过", syncType, employeeId);
                    erpSyncService.markSyncSkipped(syncType, employeeId, staffName, staffNo);
                    return new int[]{0, 0, 1};
            }

            erpSyncService.markSyncSuccess(syncType, employeeId, staffName, staffNo, erpObjectId);
            log.debug("同步成功: {} {} ({}), erpObjectId={}", syncType, staffName, staffNo, erpObjectId);
            return new int[]{1, 0, 0};

        } catch (Exception e) {
            String errMsg = e.getMessage();
            log.error("同步失败: syncType={}, staffNo={}, staffName={}, employeeId={}, error={}",
                    syncType, staffNo, staffName, employeeId, errMsg, e);
            erpSyncService.markSyncFailed(syncType, employeeId, staffName, staffNo, errMsg);
            return new int[]{0, 1, 0};
        }
    }

    /**
     * 同步单个人员到伯俊ERP
     *
     * @param syncType   同步类型: ONBOARDING / UPDATE / LEAVING
     * @param employeeId IHR员工ID
     * @return null=成功, 非null=错误信息
     */
    public String syncSingle(String syncType, String employeeId) {
        log.info("手动同步单个人员到伯俊ERP, syncType={}, employeeId={}", syncType, employeeId);

        // 离职接口暂未提供
        if ("LEAVING".equals(syncType)) {
            return "ERP离职同步接口暂未提供";
        }

        try {
            IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
            if (detail == null) {
                String err = "员工详情未找到: " + employeeId;
                erpSyncService.markSyncFailed(syncType, employeeId, "", "", err);
                return err;
            }

            String staffName = detail.getStaffName();
            String staffNo = detail.getStaffNo();

            Long erpObjectId = null;
            switch (syncType) {
                case "ONBOARDING":
                    erpObjectId = syncOnboarding(detail);
                    break;
                case "UPDATE":
                    erpObjectId = syncUpdate(detail);
                    break;
                case "LEAVING":
                    erpObjectId = syncLeaving(detail);
                    break;
                default:
                    String err = "未知同步类型: " + syncType;
                    erpSyncService.markSyncFailed(syncType, employeeId, staffName, staffNo, err);
                    return err;
            }

            // 成功：写入同步状态
            erpSyncService.markSyncSuccess(syncType, employeeId, staffName, staffNo, erpObjectId);
            return null;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            log.error("单人同步失败: syncType={}, employeeId={}, error={}", syncType, employeeId, errMsg, e);
            // 失败：写入同步状态
            try {
                IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
                String staffName = detail != null ? detail.getStaffName() : "";
                String staffNo = detail != null ? detail.getStaffNo() : "";
                erpSyncService.markSyncFailed(syncType, employeeId, staffName, staffNo, errMsg);
            } catch (Exception ex) {
                log.error("写入同步失败状态异常", ex);
            }
            return errMsg;
        }
    }

    // ==================== 具体同步逻辑 ====================

    /**
     * 入职同步 - ObjectCreate 新增员工到伯俊ERP
     *
     * @return erpObjectId（伯俊返回的记录ID），失败返回null
     */
    private Long syncOnboarding(IhrEmployeeDetail detail) {
        JSONObject data = IhrToBjErpConverter.toCreateParams(detail);
        log.info("入职同步请求: staffNo={}, staffName={}, params={}", detail.getStaffNo(), detail.getStaffName(), data);
        JSONArray resp = bjErpApiClient.call(buildTransactions("ObjectCreate", data));
        if (!BjErpApiClient.isSuccess(resp)) {
            String errMsg = BjErpApiClient.extractErrorMessage(resp);
            log.warn("入职同步失败: staffNo={}, staffName={}, error={}", detail.getStaffNo(), detail.getStaffName(), errMsg);
            throw new RuntimeException(errMsg);
        }
        Long objectId = BjErpApiClient.extractObjectId(resp);
        log.info("入职同步成功: staffNo={}, staffName={}, erpObjectId={}", detail.getStaffNo(), detail.getStaffName(), objectId);
        return objectId;
    }

    /**
     * 变更同步 - ObjectModify 增量更新伯俊ERP员工
     * 伯俊框架通过params中的ak（员工工号）自动定位记录
     *
     * @return erpObjectId（伯俊返回的记录ID），失败返回null
     */
    private Long syncUpdate(IhrEmployeeDetail detail) {
        JSONObject data = IhrToBjErpConverter.toModifyParams(detail);
        log.debug("变更同步: staffNo={}, staffName={}, params={}", detail.getStaffNo(), detail.getStaffName(), data);
        JSONArray resp = bjErpApiClient.call(buildTransactions("ObjectModify", data));
        if (!BjErpApiClient.isSuccess(resp)) {
            String errMsg = BjErpApiClient.extractErrorMessage(resp);
            log.warn("变更同步失败: staffNo={}, staffName={}, error={}", detail.getStaffNo(), detail.getStaffName(), errMsg);
            throw new RuntimeException(errMsg);
        }
        Long objectId = BjErpApiClient.extractObjectId(resp);
        log.info("变更同步成功: staffNo={}, staffName={}, erpObjectId={}", detail.getStaffNo(), detail.getStaffName(), objectId);
        return objectId;
    }

    /**
     * 离职同步 - ObjectModify 更新伯俊ERP员工为离职状态
     * 伯俊框架通过params中的ak（员工工号）自动定位记录
     *
     * @return erpObjectId（伯俊返回的记录ID），失败返回null
     */
    private Long syncLeaving(IhrEmployeeDetail detail) {
        JSONObject data = IhrToBjErpConverter.toLeavingParams(detail);
        log.debug("离职同步: staffNo={}, staffName={}, params={}", detail.getStaffNo(), detail.getStaffName(), data);
        JSONArray resp = bjErpApiClient.call(buildTransactions("ObjectModify", data));
        if (!BjErpApiClient.isSuccess(resp)) {
            String errMsg = BjErpApiClient.extractErrorMessage(resp);
            log.warn("离职同步失败: staffNo={}, staffName={}, error={}", detail.getStaffNo(), detail.getStaffName(), errMsg);
            throw new RuntimeException(errMsg);
        }
        Long objectId = BjErpApiClient.extractObjectId(resp);
        log.info("离职同步成功: staffNo={}, staffName={}, erpObjectId={}", detail.getStaffNo(), detail.getStaffName(), objectId);
        return objectId;
    }

    /**
     * 构建伯俊transactions
     * 默认使用员工表 12462
     */
    private JSONArray buildTransactions(String command, JSONObject params) {
        if (!params.containsKey("table")) {
            params.set("table", "12462");
        }
        JSONObject transaction = new JSONObject();
        transaction.set("id", java.util.UUID.randomUUID().toString());
        transaction.set("command", command);
        transaction.set("params", params);
        JSONArray transactions = new JSONArray();
        transactions.add(transaction);
        return transactions;
    }
}
