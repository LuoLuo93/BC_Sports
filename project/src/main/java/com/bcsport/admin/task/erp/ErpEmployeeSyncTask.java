package com.bcsport.admin.task.erp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.erpmapper.BjerpEmployeeMapper;
import com.bcsport.admin.ihrmapper.ErpEmployeeSyncStatusMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.service.ErpEmployeeSyncService;
import com.bcsport.admin.vo.ErpEmployeeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * ERP员工同步任务
 * <p>
 * 数据流: IHR员工数据 → 伯俊ERP(表14630)
 * <p>
 * 2026-08-18 伯俊升级 REST v3：ObjectModify 的 ak(工号)定位废弃，
 * 改为 HR_EMPLOYEE 按工号查记录ID → ObjectModify(id + partial_update=true) 只更新差异字段。
 * <ul>
 *   <li>入职(ObjectCreate): 新增员工到ERP（协议未变，NO=工号）</li>
 *   <li>变更(ObjectModify): 查现值→比对→只发差异字段；ERP无此工号降级入职创建</li>
 *   <li>离职(ObjectModify): 更新ERP员工为离职状态（已同步迁移id协议，功能暂禁用）</li>
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

    @Autowired
    private BjerpEmployeeMapper bjerpEmployeeMapper;

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
            // 时间窗口：今天 + 昨天（对齐企微同步，避免全表扫描历史数据）
            // 漏数据时可通过管理页面手动点击 syncSingle 补偿
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = cal.getTime();
            List<Date> syncDates = Arrays.asList(yesterday, today);

            // 1. 入职同步（限最近2天，且 effective sync status = 0/2）
            List<ErpEmployeeVO> onboardings = syncStatusMapper.selectPendingOnboardings(syncDates);
            log.info("待同步入职人员(最近2天): {}", onboardings.size());
            for (ErpEmployeeVO vo : onboardings) {
                int[] result = syncOne("ONBOARDING", vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo());
                successCount += result[0];
                failCount += result[1];
                skipCount += result[2];
            }

            // 2. 变更同步（限最近2天，且 effective sync status = 0/2）
            List<ErpEmployeeVO> updates = syncStatusMapper.selectPendingUpdates(syncDates);
            log.info("待同步变更人员(最近2天): {}", updates.size());
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

            SyncResult result;
            switch (syncType) {
                case "ONBOARDING":
                    result = syncOnboarding(detail);
                    break;
                case "UPDATE":
                    result = syncUpdate(detail);
                    break;
                case "LEAVING":
                    result = syncLeaving(detail);
                    break;
                default:
                    log.warn("未知同步类型: {}, employeeId={}, 跳过", syncType, employeeId);
                    erpSyncService.markSyncSkipped(syncType, employeeId, staffName, staffNo);
                    return new int[]{0, 0, 1};
            }

            erpSyncService.markSyncSuccess(syncType, employeeId, staffName, staffNo, result.erpObjectId);
            // 记录成功日志（含完整请求体/响应体）
            erpSyncService.recordLog(syncType, employeeId, staffName, staffNo, 1,
                    result.requestBody, result.responseBody, null);
            log.debug("同步成功: {} {} ({}), erpObjectId={}", syncType, staffName, staffNo, result.erpObjectId);
            return new int[]{1, 0, 0};

        } catch (Exception e) {
            String errMsg = e.getMessage();
            // 从异常中提取请求/响应体（业务错误或同步方法抛出）
            String reqBody = extractReqBody(e);
            String respBody = extractRespBody(e);
            // 入职报"编号已存在" → 幂等防重，标记已跳过(3)；
            // 变更走到的"已存在"（工号在ERP但按工号查不到现值）属异常态 → 按失败(2)上抛重试，不静默跳过
            if (BjErpApiClient.isAlreadyExists(errMsg) && "ONBOARDING".equals(syncType)) {
                log.info("同步跳过(数据已存在): syncType={}, staffNo={}, staffName={}, employeeId={}",
                        syncType, staffNo, staffName, employeeId);
                erpSyncService.markSyncSkipped(syncType, employeeId, staffName, staffNo);
                // 已跳过也记日志，方便排查（状态3）
                erpSyncService.recordLog(syncType, employeeId, staffName, staffNo, 3, reqBody, respBody, errMsg);
                return new int[]{0, 0, 1};
            }
            log.error("同步失败: syncType={}, staffNo={}, staffName={}, employeeId={}, error={}",
                    syncType, staffNo, staffName, employeeId, errMsg, e);
            erpSyncService.markSyncFailed(syncType, employeeId, staffName, staffNo, errMsg);
            // 失败也记日志（含请求/响应体），状态2
            erpSyncService.recordLog(syncType, employeeId, staffName, staffNo, 2, reqBody, respBody, errMsg);
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

            // 离职状态(LEAVED/QUIT)不同步到ERP
            String staffStatus = detail.getStaffStatus();
            if ("LEAVED".equals(staffStatus) || "QUIT".equals(staffStatus)) {
                String msg = "员工已离职(" + staffStatus + "), 跳过同步";
                erpSyncService.markSyncSkipped(syncType, employeeId, staffName, staffNo);
                return msg;
            }

            SyncResult result;
            switch (syncType) {
                case "ONBOARDING":
                    result = syncOnboarding(detail);
                    break;
                case "UPDATE":
                    result = syncUpdate(detail);
                    break;
                case "LEAVING":
                    result = syncLeaving(detail);
                    break;
                default:
                    String err = "未知同步类型: " + syncType;
                    erpSyncService.markSyncFailed(syncType, employeeId, staffName, staffNo, err);
                    return err;
            }

            // 成功：写入同步状态 + 记录日志
            erpSyncService.markSyncSuccess(syncType, employeeId, staffName, staffNo, result.erpObjectId);
            erpSyncService.recordLog(syncType, employeeId, staffName, staffNo, 1,
                    result.requestBody, result.responseBody, null);
            return null;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            String reqBody = extractReqBody(e);
            String respBody = extractRespBody(e);
            try {
                IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
                String staffName = detail != null ? detail.getStaffName() : "";
                String staffNo = detail != null ? detail.getStaffNo() : "";
                // 入职报"编号已存在" → 幂等防重标记跳过(3)；变更报"已存在"按失败返回（不再静默跳过）
                if (BjErpApiClient.isAlreadyExists(errMsg) && "ONBOARDING".equals(syncType)) {
                    log.info("单人同步跳过(数据已存在): syncType={}, employeeId={}", syncType, employeeId);
                    erpSyncService.markSyncSkipped(syncType, employeeId, staffName, staffNo);
                    erpSyncService.recordLog(syncType, employeeId, staffName, staffNo, 3, reqBody, respBody, errMsg);
                    return "数据已存在，已跳过";
                }
                log.error("单人同步失败: syncType={}, employeeId={}, error={}", syncType, employeeId, errMsg, e);
                erpSyncService.markSyncFailed(syncType, employeeId, staffName, staffNo, errMsg);
                erpSyncService.recordLog(syncType, employeeId, staffName, staffNo, 2, reqBody, respBody, errMsg);
            } catch (Exception ex) {
                log.error("写入同步失败状态异常", ex);
            }
            return errMsg;
        }
    }

    // ==================== 具体同步逻辑 ====================

    /**
     * 入职同步 - ObjectCreate 新增员工到伯俊ERP
     */
    private SyncResult syncOnboarding(IhrEmployeeDetail detail) {
        JSONObject data = IhrToBjErpConverter.toCreateParams(detail);
        log.info("入职同步请求: staffNo={}, staffName={}, params={}", detail.getStaffNo(), detail.getStaffName(), data);
        BjErpApiClient.CallRecord record = bjErpApiClient.call(buildTransactions("ObjectCreate", data));
        Long objectId = BjErpApiClient.extractObjectId(record.getResponse());
        log.info("入职同步成功: staffNo={}, staffName={}, erpObjectId={}", detail.getStaffNo(), detail.getStaffName(), objectId);
        return new SyncResult(objectId, record.getRequestBody(), record.getResponseBody());
    }

    /**
     * 变更同步 - REST v3 ObjectModify（id 数字定位 + 全字段 IHR 最新覆盖）
     * <p>
     * 2026-08-18 伯俊升级后 ak(工号) 定位废弃，流程改为：
     * <ol>
     *   <li>HR_EMPLOYEE 按工号查记录 ID</li>
     *   <li>查到：先校验 IHR 部门名在 ERP 店仓精确存在（漂移如淮南/济南则明确失败提示人工核正），
     *       然后全字段按 IHR 最新数据覆盖（不做字段级差异比对）</li>
     *   <li>查不到：工号在 ERP 无记录 → 降级走入职创建（真新人场景，与企微自动入职对齐）；
     *       创建又报"已存在"则重查一次补修改，仍查不到按失败上抛（不再静默跳过）</li>
     * </ol>
     */
    private SyncResult syncUpdate(IhrEmployeeDetail detail) {
        Long erpId = bjerpEmployeeMapper.selectIdByStaffNo(detail.getStaffNo());
        if (erpId == null) {
            log.info("员工 {}({}) 在伯俊ERP无记录，降级走入职创建", detail.getStaffName(), detail.getStaffNo());
            try {
                return syncOnboarding(detail);
            } catch (BjErpApiClient.BusinessCallException createEx) {
                if (!BjErpApiClient.isAlreadyExists(createEx.getMessage())) {
                    throw createEx;
                }
                // 创建报"已存在"但按工号又查不到（NO 带空格/竞态等）→ 重查一次，查到即走修改
                Long retryId = bjerpEmployeeMapper.selectIdByStaffNo(detail.getStaffNo());
                if (retryId != null) {
                    log.info("员工 {}({}) 创建报已存在但重查命中(ID={})，转走修改",
                            detail.getStaffName(), detail.getStaffNo(), retryId);
                    return doModify(detail, retryId);
                }
                throw createEx;
            }
        }
        return doModify(detail, erpId);
    }

    /**
     * 全字段覆盖修改：以 IHR 最新数据为准，partial_update=true 保护未传字段
     */
    private SyncResult doModify(IhrEmployeeDetail detail, Long erpId) {
        // 报文必含店仓字段，IHR 部门名在 ERP 店仓不存在时明确失败等人工核正（店名漂移/新店未建），不静默跳过
        String ihrDept = detail.getDepartmentName() == null ? "" : detail.getDepartmentName().trim();
        if (!ihrDept.isEmpty() && bjerpEmployeeMapper.countActiveStoreByName(ihrDept) == 0) {
            throw new BjErpApiClient.BusinessCallException(
                    "IHR部门名[" + ihrDept + "]在ERP店仓不存在(疑似店名漂移或新店未建), 请人工核正后重试", null, null);
        }

        JSONObject data = IhrToBjErpConverter.toModifyParams(detail, erpId);
        log.debug("变更同步: staffNo={}, staffName={}, erpId={}, params={}",
                detail.getStaffNo(), detail.getStaffName(), erpId, data);
        BjErpApiClient.CallRecord record = bjErpApiClient.call(buildTransactions("ObjectModify", data));
        // 修改响应不含 objectid，用工号定位查询到的 ID 回填 erp_object_id
        log.info("变更同步成功: staffNo={}, staffName={}, erpId={}", detail.getStaffNo(), detail.getStaffName(), erpId);
        return new SyncResult(erpId, record.getRequestBody(), record.getResponseBody());
    }

    /**
     * 离职同步 - ObjectModify 更新伯俊ERP员工为离职状态（REST v3 id 协议，功能仍禁用）
     */
    private SyncResult syncLeaving(IhrEmployeeDetail detail) {
        Long erpId = bjerpEmployeeMapper.selectIdByStaffNo(detail.getStaffNo());
        if (erpId == null) {
            throw new BjErpApiClient.BusinessCallException(
                    "员工 " + detail.getStaffName() + "(" + detail.getStaffNo() + ") 在伯俊ERP无记录, 无法置离职", null, null);
        }
        JSONObject data = IhrToBjErpConverter.toLeavingParams(detail, erpId);
        log.debug("离职同步: staffNo={}, staffName={}, erpId={}, params={}",
                detail.getStaffNo(), detail.getStaffName(), erpId, data);
        BjErpApiClient.CallRecord record = bjErpApiClient.call(buildTransactions("ObjectModify", data));
        log.info("离职同步成功: staffNo={}, staffName={}, erpId={}", detail.getStaffNo(), detail.getStaffName(), erpId);
        return new SyncResult(erpId, record.getRequestBody(), record.getResponseBody());
    }

    /**
     * 构建伯俊transactions
     * 默认使用员工表 14630
     */
    private JSONArray buildTransactions(String command, JSONObject params) {
        if (!params.containsKey("table")) {
            params.set("table", "14630");
        }
        JSONObject transaction = new JSONObject();
        transaction.set("id", java.util.UUID.randomUUID().toString());
        transaction.set("command", command);
        transaction.set("params", params);
        JSONArray transactions = new JSONArray();
        transactions.add(transaction);
        return transactions;
    }

    // ==================== 内部数据载体 ====================

    /**
     * 同步成功结果：持有 erpObjectId + 完整请求体/响应体（用于日志落库）
     */
    private static class SyncResult {
        final Long erpObjectId;
        final String requestBody;
        final String responseBody;

        SyncResult(Long erpObjectId, String requestBody, String responseBody) {
            this.erpObjectId = erpObjectId;
            this.requestBody = requestBody;
            this.responseBody = responseBody;
        }
    }

    /**
     * 从异常中提取请求体（仅 BusinessCallException 携带请求/响应体）
     */
    private static String extractReqBody(Exception e) {
        if (e instanceof BjErpApiClient.BusinessCallException) {
            return ((BjErpApiClient.BusinessCallException) e).getRequestBody();
        }
        return null;
    }

    /**
     * 从异常中提取响应体（仅 BusinessCallException 携带请求/响应体）
     */
    private static String extractRespBody(Exception e) {
        if (e instanceof BjErpApiClient.BusinessCallException) {
            return ((BjErpApiClient.BusinessCallException) e).getResponseBody();
        }
        return null;
    }
}
