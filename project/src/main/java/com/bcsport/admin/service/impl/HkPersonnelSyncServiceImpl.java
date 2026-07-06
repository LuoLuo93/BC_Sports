package com.bcsport.admin.service.impl;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.HkEmployeeQueryDTO;
import com.bcsport.admin.entity.hkerp.HkBasPersonnel;
import com.bcsport.admin.entity.hkerp.HkShopStockSportCity;
import com.bcsport.admin.entity.ihr.HkErpSyncStatus;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import com.bcsport.admin.hkerpmapper.HkBasPersonnelMapper;
import com.bcsport.admin.ihrmapper.HkErpSyncStatusMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeDetailMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.service.HkPersonnelSyncService;
import com.bcsport.admin.task.hkerp.IhrToHkConverter;
import com.bcsport.admin.vo.ErpEmployeeVO;
import com.bcsport.admin.vo.HkSyncStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 旧版HK ERP 职员资料同步服务实现
 * <p>
 * 数据源：BC_SPORTS_IHR 库（与伯俊链路同一份员工数据）
 * 写入：HKERP 库 Bas_Personnel（直写，单人单条 insert）
 * 部门过滤：终端店铺递归
 * 同步状态：BC_SPORTS_IHR 库 hk_erp_sync_status（HK_ 前缀）
 * <p>
 * 跨数据源事务：状态记录走 ihrTransactionManager，Bas_Personnel 写入走 hkerpTransactionManager。
 */
@Slf4j
@Service
public class HkPersonnelSyncServiceImpl implements HkPersonnelSyncService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ===== 同步类型常量 =====
    private static final String TYPE_ONBOARDING = "HK_ONBOARDING";
    private static final String TYPE_UPDATE = "HK_UPDATE";
    private static final String TYPE_LEAVING = "HK_LEAVING";

    // ===== 同步状态码 =====
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILED = 2;
    private static final int STATUS_SKIPPED = 3;

    @Autowired
    private HkErpSyncStatusMapper hkSyncStatusMapper;

    @Autowired
    private IhrEmployeeDetailMapper employeeDetailMapper;

    @Autowired
    private IhrEmployeeExclusionMapper exclusionMapper;

    @Autowired
    private HkBasPersonnelMapper basPersonnelMapper;

    @Autowired
    private IhrToHkConverter converter;

    @Autowired
    @Qualifier("ihrTransactionManager")
    private PlatformTransactionManager ihrTransactionManager;

    @Autowired
    @Qualifier("hkerpTransactionManager")
    private PlatformTransactionManager hkerpTransactionManager;

    // ==================== 同步主流程 ====================

    /**
     * 新员工入职同步
     */
    @Override
    public HkSyncStatsVO syncNewPersonnel() {
        HkSyncStatsVO stats = new HkSyncStatsVO();
        List<String> shopDeptIds = hkSyncStatusMapper.selectShopDepartmentIds();
        if (shopDeptIds.isEmpty()) {
            stats.setMessage("未找到终端店铺部门，跳过入职同步");
            return stats;
        }

        List<ErpEmployeeVO> pending = hkSyncStatusMapper.selectPendingHkOnboardings(shopDeptIds);
        log.info("HKERP入职同步：待同步 {} 人", pending.size());

        // PersonnelID：批量开始时取一次当前 Max，解析出前缀和序号，批量内前缀固定、序号内存递增，不逐人查库
        String maxNum = basPersonnelMapper.queryMaxNum();
        String personnelPrefix = IhrToHkConverter.extractPrefix(maxNum);
        if (personnelPrefix == null) {
            log.warn("queryMaxNum 返回无效值：{}，PersonnelID 用默认前缀", maxNum);
            personnelPrefix = "BC";
        }
        AtomicLong personnelSeq = new AtomicLong(IhrToHkConverter.extractSeq(maxNum));

        for (ErpEmployeeVO vo : pending) {
            try {
                OneResult r = syncOneOnboarding(vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo(),
                        personnelPrefix, personnelSeq);
                switch (r.outcome) {
                    case CREATED:    stats.incrOnboarded();   break;
                    case RE_ONBOARDED: stats.incrReOnboarded(); break;
                    case SKIPPED:    stats.incrSkipped();    break;
                    case FAILED:     stats.incrFailed();     break;
                    default: break;
                }
            } catch (Exception e) {
                log.error("HKERP入职同步失败：{} {} {}", vo.getStaffName(), vo.getStaffNo(), e.getMessage(), e);
                markSyncFailed(TYPE_ONBOARDING, vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo(), e.getMessage());
                stats.incrError();
            }
        }

        stats.setMessage(String.format("入职同步完成：新增%d, 二次入职%d, 跳过%d, 失败%d",
                stats.getOnboarded(), stats.getReOnboarded(), stats.getSkipped(), stats.getFailed()));
        return stats;
    }

    /**
     * 员工变更 + 离职同步
     */
    @Override
    public HkSyncStatsVO syncPersonnelUpdate() {
        HkSyncStatsVO stats = new HkSyncStatsVO();
        List<String> shopDeptIds = hkSyncStatusMapper.selectShopDepartmentIds();
        if (shopDeptIds.isEmpty()) {
            stats.setMessage("未找到终端店铺部门，跳过变更离职同步");
            return stats;
        }

        // ========== 1. 变更同步 ==========
        List<ErpEmployeeVO> updates = hkSyncStatusMapper.selectPendingHkUpdates(shopDeptIds);
        log.info("HKERP变更同步：待同步 {} 人", updates.size());
        for (ErpEmployeeVO vo : updates) {
            try {
                syncOneUpdate(vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo(), stats);
            } catch (Exception e) {
                log.error("HKERP变更同步失败：{} {} {}", vo.getStaffName(), vo.getStaffNo(), e.getMessage(), e);
                markSyncFailed(TYPE_UPDATE, vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo(), e.getMessage());
                stats.incrError();
            }
        }

        // ========== 2. 离职同步 ==========
        List<ErpEmployeeVO> leavings = hkSyncStatusMapper.selectPendingHkLeavings(shopDeptIds);
        log.info("HKERP离职同步：待同步 {} 人", leavings.size());
        for (ErpEmployeeVO vo : leavings) {
            try {
                OneResult r = syncOneLeaving(vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo());
                if (r.outcome == Outcome.DISABLED) {
                    stats.incrLeaveDisabled();
                }
            } catch (Exception e) {
                log.error("HKERP离职同步失败：{} {} {}", vo.getStaffName(), vo.getStaffNo(), e.getMessage(), e);
                markSyncFailed(TYPE_LEAVING, vo.getEmployeeId(), vo.getStaffName(), vo.getStaffNo(), e.getMessage());
                stats.incrError();
            }
        }

        // ========== 3. 离职收尾：满30天改在职状态（直读 HKERP Bas_Personnel） ==========
        finalizeLeaving(stats);

        stats.setMessage(String.format("变更与离职同步完成：手机号变更%d, 部门变更%d, 离职禁用%d, 离职收尾%d, 异常%d",
                stats.getPhoneUpdated(), stats.getShopUpdated(),
                stats.getLeaveDisabled(), stats.getLeaveFinalized(), stats.getErrorCount()));
        return stats;
    }

    // ==================== 分页查询（前端 Tab 展示） ====================

    @Override
    public PageResult<ErpEmployeeVO> pageHkOnboardings(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO) {
        return doPage(queryDTO, 1, pageQuery, (qp, offset, limit) -> {
            long total = hkSyncStatusMapper.countHkOnboarding(qp.staffName, qp.staffNo, qp.syncStatus, qp.startDate, qp.endDate, qp.excludedStaffNos, qp.shopDeptIds);
            List<ErpEmployeeVO> records = hkSyncStatusMapper.selectHkOnboardingPage(qp.staffName, qp.staffNo, qp.syncStatus, qp.startDate, qp.endDate, qp.excludedStaffNos, qp.shopDeptIds, offset, limit);
            return new PageData(total, records);
        });
    }

    @Override
    public PageResult<ErpEmployeeVO> pageHkUpdates(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO) {
        return doPage(queryDTO, 1, pageQuery, (qp, offset, limit) -> {
            long total = hkSyncStatusMapper.countHkUpdate(qp.staffName, qp.staffNo, qp.syncStatus, qp.startDate, qp.endDate, qp.excludedStaffNos, qp.shopDeptIds);
            List<ErpEmployeeVO> records = hkSyncStatusMapper.selectHkUpdatePage(qp.staffName, qp.staffNo, qp.syncStatus, qp.startDate, qp.endDate, qp.excludedStaffNos, qp.shopDeptIds, offset, limit);
            return new PageData(total, records);
        });
    }

    @Override
    public PageResult<ErpEmployeeVO> pageHkLeavings(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO) {
        return doPage(queryDTO, 2, pageQuery, (qp, offset, limit) -> {
            long total = hkSyncStatusMapper.countHkLeaving(qp.staffName, qp.staffNo, qp.syncStatus, qp.startDate, qp.endDate, qp.excludedStaffNos, qp.shopDeptIds);
            List<ErpEmployeeVO> records = hkSyncStatusMapper.selectHkLeavingPage(qp.staffName, qp.staffNo, qp.syncStatus, qp.startDate, qp.endDate, qp.excludedStaffNos, qp.shopDeptIds, offset, limit);
            return new PageData(total, records);
        });
    }

    // ==================== 单人处理（批量与单条共用） ====================

    /**
     * 入职同步单人处理：判重 → 二次入职 → 全新员工(组装+单条insert)。同步状态真实反映结果。
     *
     * @param personnelPrefix PersonnelID 前2位前缀（批量内固定）
     * @param personnelSeq    PersonnelID 序号递增器（调用一次自增一次）
     */
    OneResult syncOneOnboarding(String employeeId, String staffName, String staffNo,
                                String personnelPrefix, AtomicLong personnelSeq) {
        IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
        if (detail == null) {
            markSyncSkipped(TYPE_ONBOARDING, employeeId, staffName, staffNo);
            return OneResult.skipped();
        }
        String mobileNo = detail.getMobileNo();

        // 1. 已在职（AllowUsed=1）→ 跳过
        if (basPersonnelMapper.findPersonnel(staffName, mobileNo)) {
            markSyncSkipped(TYPE_ONBOARDING, employeeId, staffName, staffNo);
            return OneResult.skipped();
        }

        // 2. 二次入职（AllowUsed=0 的旧记录）→ 修改在职状态 + 调部门/仓库
        if (basPersonnelMapper.findPersonnelByNameAndMobileNo(staffName, mobileNo) > 0) {
            return handleReOnboard(detail, employeeId, staffName, staffNo);
        }

        // 3. 全新员工 → 组装 + 单条 insert（与源项目逐条 insert 一致），状态真实
        try {
            HkBasPersonnel assembled = converter.toCreate(detail, personnelPrefix, personnelSeq);
            hkerpTx(() -> basPersonnelMapper.insertBatch(Collections.singletonList(assembled)));
            log.info("{} 全新入职，已录入 Bas_Personnel, personnelId={}", staffName, assembled.getPersonnelId());
            markSyncSuccess(TYPE_ONBOARDING, employeeId, staffName, staffNo);
            return OneResult.created();
        } catch (Exception e) {
            String err = "录入 Bas_Personnel 失败：" + e.getMessage();
            log.error("{} 全新入职录入失败：{}", staffName, e.getMessage(), e);
            markSyncFailed(TYPE_ONBOARDING, employeeId, staffName, staffNo, err);
            return OneResult.failed(err);
        }
    }

    /** 二次入职：查不到 ShopID 视为失败（如实反映），否则改状态+调部门 */
    private OneResult handleReOnboard(IhrEmployeeDetail detail, String employeeId, String staffName, String staffNo) {
        String mobileNo = detail.getMobileNo();
        HkShopStockSportCity shop = basPersonnelMapper.queryShopIdByDepartmentName(detail.getDepartmentName());
        if (shop == null) {
            String err = "bas_shop 无对应名称的商铺：" + detail.getDepartmentName();
            log.info("{} 二次入职，但 bas_shop 无对应商铺 {}，状态修改失败", staffName, detail.getDepartmentName());
            markSyncFailed(TYPE_ONBOARDING, employeeId, staffName, staffNo, err);
            return OneResult.failed(err);
        }
        try {
            hkerpTx(() -> basPersonnelMapper.updatePersonnelOfAllowUsedAndPersonnelStatus(
                    staffName, mobileNo, shop.getShopId(), shop.getStockId()));
            log.info("{} 二次入职，已调整到部门 {} shopId={}", staffName, detail.getDepartmentName(), shop.getShopId());
            markSyncSuccess(TYPE_ONBOARDING, employeeId, staffName, staffNo);
            return OneResult.reOnboarded();
        } catch (Exception e) {
            String err = "二次入职状态修改失败：" + e.getMessage();
            log.error("{} 二次入职状态修改失败：{}", staffName, e.getMessage(), e);
            markSyncFailed(TYPE_ONBOARDING, employeeId, staffName, staffNo, err);
            return OneResult.failed(err);
        }
    }

    /**
     * 变更同步单人处理：更新手机号 + 按当前部门更新 ShopID/StockID。统计如实反映。
     */
    void syncOneUpdate(String employeeId, String staffName, String staffNo, HkSyncStatsVO stats) {
        IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
        if (detail == null) {
            markSyncSkipped(TYPE_UPDATE, employeeId, staffName, staffNo);
            return;
        }
        String mobileNo = detail.getMobileNo();
        String departmentName = detail.getDepartmentName();

        // 部门/店铺更新：查不到 ShopID 时跳过店铺更新（不计入"部门变更"）
        String shopId = basPersonnelMapper.findShopId(departmentName);
        if (shopId != null) {
            String stockId = basPersonnelMapper.getStockIdByShopId(shopId);
            hkerpTx(() -> basPersonnelMapper.updateUserShopId(
                    staffName, mobileNo, shopId, mobileNo, shopId, departmentName, stockId));
            log.info("{} 部门/店铺已更新为 {}", staffName, departmentName);
            stats.incrShopUpdated();
        } else {
            log.info("{} 部门 {} 在 bas_shop 未查到 ShopID，跳过店铺更新", staffName, departmentName);
        }

        // 手机号更新：按当前 IHR 的 mobileNo 更新（updateUserPh 内部按 Mobilephone 或 Telphone 定位）
        hkerpTx(() -> basPersonnelMapper.updateUserPh(staffName, mobileNo, mobileNo));
        stats.incrPhoneUpdated();

        markSyncSuccess(TYPE_UPDATE, employeeId, staffName, staffNo);
    }

    /**
     * 离职同步单人处理：仅当当前在职（AllowUsed=1）才执行禁用。
     */
    OneResult syncOneLeaving(String employeeId, String staffName, String staffNo) {
        IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
        if (detail == null) {
            markSyncSkipped(TYPE_LEAVING, employeeId, staffName, staffNo);
            return OneResult.skipped();
        }
        String mobileNo = detail.getMobileNo();
        if (basPersonnelMapper.findPersonnel(staffName, mobileNo)) {
            hkerpTx(() -> basPersonnelMapper.updateAllowUse(staffName, mobileNo));
            log.info("{} 已禁用（AllowUsed=0, PersonnelStatus=2）", staffName);
            markSyncSuccess(TYPE_LEAVING, employeeId, staffName, staffNo);
            return OneResult.disabled();
        }
        // 已不在职，无需重复禁用
        markSyncSuccess(TYPE_LEAVING, employeeId, staffName, staffNo);
        return OneResult.updated();
    }

    /**
     * 离职收尾：满30天改在职状态（直读 HKERP Bas_Personnel）。
     */
    private void finalizeLeaving(HkSyncStatsVO stats) {
        try {
            List<HkBasPersonnel> pending = basPersonnelMapper.findPersonnelByAllowUseAndStatus();
            if (pending.isEmpty()) {
                return;
            }
            log.info("HKERP离职收尾：发现 {} 名员工禁用满30天待转离职", pending.size());
            for (HkBasPersonnel p : pending) {
                try {
                    hkerpTx(() -> basPersonnelMapper.updateStatus(p.getPersonnelName(), p.getMobilePhone()));
                    log.info("{} 满30天，PersonnelStatus 改为离职", p.getPersonnelName());
                    stats.incrLeaveFinalized();
                } catch (Exception e) {
                    log.error("HKERP离职收尾失败：{} {}", p.getPersonnelName(), e.getMessage(), e);
                    stats.incrError();
                }
            }
        } catch (Exception e) {
            log.error("HKERP离职收尾查询失败：{}", e.getMessage(), e);
            stats.incrError();
        }
    }

    /**
     * 单条同步（前端单条触发）。
     *
     * @param syncType   HK_ONBOARDING / HK_UPDATE / HK_LEAVING
     * @param employeeId IHR员工ID
     * @return null=成功，非空=错误信息
     */
    @Override
    public String syncSingle(String syncType, String employeeId) {
        log.info("HK ERP单条同步: syncType={}, employeeId={}", syncType, employeeId);
        try {
            IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
            String staffName = detail != null ? detail.getStaffName() : "";
            String staffNo = detail != null ? detail.getStaffNo() : "";

            switch (syncType) {
                case TYPE_ONBOARDING: {
                    String maxNum = basPersonnelMapper.queryMaxNum();
                    String prefix = IhrToHkConverter.extractPrefix(maxNum);
                    if (prefix == null) {
                        prefix = "BC";
                    }
                    AtomicLong seq = new AtomicLong(IhrToHkConverter.extractSeq(maxNum));
                    OneResult r = syncOneOnboarding(employeeId, staffName, staffNo, prefix, seq);
                    return r.errorMessage;
                }
                case TYPE_UPDATE: {
                    HkSyncStatsVO sink = new HkSyncStatsVO();
                    syncOneUpdate(employeeId, staffName, staffNo, sink);
                    return null;
                }
                case TYPE_LEAVING: {
                    OneResult r = syncOneLeaving(employeeId, staffName, staffNo);
                    return r.errorMessage;
                }
                default:
                    String err = "未知同步类型: " + syncType;
                    markSyncFailed(syncType, employeeId, staffName, staffNo, err);
                    return err;
            }
        } catch (Exception e) {
            log.error("HK ERP单条同步失败: syncType={}, employeeId={}", syncType, employeeId, e);
            try {
                IhrEmployeeDetail detail = employeeDetailMapper.selectById(employeeId);
                markSyncFailed(syncType, employeeId,
                        detail != null ? detail.getStaffName() : "",
                        detail != null ? detail.getStaffNo() : "",
                        e.getMessage());
            } catch (Exception ex) {
                log.error("写入同步失败状态异常", ex);
            }
            return e.getMessage();
        }
    }

    // ==================== 内部工具 ====================

    private void markSyncSuccess(String syncType, String employeeId, String staffName, String staffNo) {
        markSync(syncType, employeeId, staffName, staffNo, STATUS_SUCCESS, null);
    }

    private void markSyncFailed(String syncType, String employeeId, String staffName, String staffNo, String errMsg) {
        markSync(syncType, employeeId, staffName, staffNo, STATUS_FAILED, errMsg);
    }

    private void markSyncSkipped(String syncType, String employeeId, String staffName, String staffNo) {
        markSync(syncType, employeeId, staffName, staffNo, STATUS_SKIPPED, null);
    }

    /** 统一写入同步状态（IHR 库事务） */
    private void markSync(String syncType, String employeeId, String staffName, String staffNo, int status, String errMsg) {
        HkErpSyncStatus s = new HkErpSyncStatus();
        s.setSyncType(syncType);
        s.setEmployeeId(employeeId);
        s.setStaffName(staffName);
        s.setStaffNo(staffNo);
        s.setSyncStatus(status);
        s.setSyncTime(new Date());
        if (errMsg != null && errMsg.length() > 500) {
            errMsg = errMsg.substring(0, 500);
        }
        s.setErrorMessage(errMsg);
        ihrTx(() -> hkSyncStatusMapper.upsertByEmployeeIdAndType(s));
    }

    /** IHR 库事务执行 */
    private void ihrTx(Runnable action) {
        new TransactionTemplate(ihrTransactionManager).execute(status -> {
            action.run();
            return null;
        });
    }

    /** HKERP 库事务执行 */
    private void hkerpTx(Runnable action) {
        new TransactionTemplate(hkerpTransactionManager).execute(status -> {
            action.run();
            return null;
        });
    }

    /** 分页查询统一封装（含默认日期、排除名单、终端店铺部门过滤） */
    private PageResult<ErpEmployeeVO> doPage(HkEmployeeQueryDTO queryDTO, int exclusionType,
                                             PageQuery pageQuery, PageFetcher fetcher) {
        QueryParams qp = buildQueryParams(queryDTO, exclusionType);
        if (qp.shopDeptIds.isEmpty()) {
            return buildPageResult(Collections.emptyList(), 0, pageQuery.getPageNum(), pageQuery.getPageSize());
        }
        long offset = (long) (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        PageData pd = fetcher.fetch(qp, offset, pageQuery.getPageSize());
        return buildPageResult(pd.records, pd.total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    private QueryParams buildQueryParams(HkEmployeeQueryDTO queryDTO, int exclusionType) {
        QueryParams qp = new QueryParams();
        if (queryDTO != null) {
            qp.staffName = queryDTO.getStaffName();
            qp.staffNo = queryDTO.getStaffNo();
            qp.syncStatus = queryDTO.getSyncStatus();
            qp.startDate = queryDTO.getStartDate();
            qp.endDate = queryDTO.getEndDate();
        }
        if ((qp.startDate == null || qp.startDate.isEmpty()) && (qp.endDate == null || qp.endDate.isEmpty())) {
            LocalDate today = LocalDate.now();
            qp.startDate = today.minusDays(30).format(DATE_FMT);
            qp.endDate = today.format(DATE_FMT);
        }
        qp.excludedStaffNos = exclusionMapper.selectActiveStaffNos(exclusionType);
        qp.shopDeptIds = hkSyncStatusMapper.selectShopDepartmentIds();
        return qp;
    }

    private PageResult<ErpEmployeeVO> buildPageResult(List<ErpEmployeeVO> records, long total, int pageNum, int pageSize) {
        PageResult<ErpEmployeeVO> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setRecords(records);
        result.setHasPrevious(pageNum > 1);
        result.setHasNext((long) pageNum < result.getPages());
        return result;
    }

    // ==================== 内部类型 ====================

    /** 单人处理结果 */
    static class OneResult {
        Outcome outcome;
        String errorMessage; // 仅 FAILED 时有值

        static OneResult created() { OneResult r = new OneResult(); r.outcome = Outcome.CREATED; return r; }
        static OneResult reOnboarded() { OneResult r = new OneResult(); r.outcome = Outcome.RE_ONBOARDED; return r; }
        static OneResult updated() { OneResult r = new OneResult(); r.outcome = Outcome.UPDATED; return r; }
        static OneResult disabled() { OneResult r = new OneResult(); r.outcome = Outcome.DISABLED; return r; }
        static OneResult skipped() { OneResult r = new OneResult(); r.outcome = Outcome.SKIPPED; return r; }
        static OneResult failed(String err) { OneResult r = new OneResult(); r.outcome = Outcome.FAILED; r.errorMessage = err; return r; }
    }

    enum Outcome { CREATED, RE_ONBOARDED, UPDATED, DISABLED, SKIPPED, FAILED }

    private static class QueryParams {
        String staffName;
        String staffNo;
        Integer syncStatus;
        String startDate;
        String endDate;
        List<String> excludedStaffNos;
        List<String> shopDeptIds;
    }

    /** 分页查询载体 */
    private static class PageData {
        final long total;
        final List<ErpEmployeeVO> records;
        PageData(long total, List<ErpEmployeeVO> records) { this.total = total; this.records = records; }
    }

    @FunctionalInterface
    private interface PageFetcher {
        PageData fetch(QueryParams qp, long offset, long limit);
    }
}
