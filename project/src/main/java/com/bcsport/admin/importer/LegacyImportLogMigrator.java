package com.bcsport.admin.importer;

import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.mapper.BudgetImportLogMapper;
import com.bcsport.admin.mapper.DwSalesImportLogMapper;
import com.bcsport.admin.mapper.EstimatedCostImportLogMapper;
import com.bcsport.admin.mapper.GoodsImportLogMapper;
import com.bcsport.admin.mapper.SysImportLogMapper;
import com.bcsport.admin.mapper.sticker.SizeGroupImportLogMapper;
import com.bcsport.admin.mapper.sticker.StickerDataImportLogMapper;
import com.bcsport.admin.qywxmapper.BasFirstAddImportLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * R01 一次性运维：把 7 张旧模块导入日志表搬入统一表。幂等——靠函数式唯一索引 (import_type, legacy_id)
 * 拒绝重复行，可在切换前/每模块切完/收尾各跑一次。生产收尾核对通过后连同旧实体/Mapper 一起删除（阶段 4）。
 * 逐行独立插入（无外层事务），单行重复或失败不影响其余行。
 */
@Slf4j
@Component
public class LegacyImportLogMigrator {

    @Autowired
    private SysImportLogMapper target;

    @Autowired
    private BasFirstAddImportLogMapper basFirstAddImportLogMapper;
    @Autowired
    private GoodsImportLogMapper goodsImportLogMapper;
    @Autowired
    private BudgetImportLogMapper budgetImportLogMapper;
    @Autowired
    private DwSalesImportLogMapper dwSalesImportLogMapper;
    @Autowired
    private EstimatedCostImportLogMapper estimatedCostImportLogMapper;
    @Autowired
    private StickerDataImportLogMapper stickerDataImportLogMapper;
    @Autowired
    private SizeGroupImportLogMapper sizeGroupImportLogMapper;

    public Map<String, Object> migrateAll() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put(ImportType.BAS_FIRST_ADD.name(),
                migrate(ImportType.BAS_FIRST_ADD, basFirstAddImportLogMapper.selectList(null), r -> r.getId()));
        report.put(ImportType.GOODS_OLD_NEW.name(),
                migrate(ImportType.GOODS_OLD_NEW, goodsImportLogMapper.selectList(null), r -> r.getId()));
        report.put(ImportType.SALES_BUDGET.name(),
                migrate(ImportType.SALES_BUDGET, budgetImportLogMapper.selectList(null), r -> r.getId()));
        report.put(ImportType.DW_SALES.name(),
                migrate(ImportType.DW_SALES, dwSalesImportLogMapper.selectList(null), r -> r.getId()));
        report.put(ImportType.ESTIMATED_COST.name(),
                migrate(ImportType.ESTIMATED_COST, estimatedCostImportLogMapper.selectList(null), r -> r.getId()));
        report.put(ImportType.STICKER_DATA.name(),
                migrate(ImportType.STICKER_DATA, stickerDataImportLogMapper.selectList(null), r -> r.getId()));
        report.put(ImportType.STICKER_SIZE_GROUP.name(),
                migrate(ImportType.STICKER_SIZE_GROUP, sizeGroupImportLogMapper.selectList(null), r -> r.getId()));
        return report;
    }

    /**
     * 旧实体字段名与 SysImportLog 完全一致（fileName/fileSize/totalCount/successCount/failCount/status/
     * errorMsg/createTime/createBy），按名拷贝；旧 id 落 legacyId，新 id 交给 IDENTITY。
     */
    private <R> Map<String, Integer> migrate(ImportType type, List<R> rows, Function<R, Number> idOf) {
        int copied = 0, skipped = 0, failed = 0;
        for (R row : rows) {
            Number legacyId = idOf.apply(row);
            if (legacyId == null) {
                skipped++;
                continue;
            }
            SysImportLog e = new SysImportLog();
            BeanUtils.copyProperties(row, e, "id");
            e.setId(null);
            e.setImportType(type.getCode());
            e.setLegacyId(legacyId.longValue());
            try {
                target.insert(e);
                copied++;
            } catch (DataAccessException ex) {
                if (isDuplicate(ex)) {
                    skipped++;
                } else {
                    failed++;
                    log.error("{} 搬迁旧日志 legacyId={} 失败: {}", type, legacyId, ex.getMessage());
                }
            }
        }
        log.info("{} 旧导入日志搬迁: source={}, copied={}, skipped={}, failed={}",
                type, rows.size(), copied, skipped, failed);
        Map<String, Integer> stat = new LinkedHashMap<>();
        stat.put("source", rows.size());
        stat.put("copied", copied);
        stat.put("skipped", skipped);
        stat.put("failed", failed);
        return stat;
    }

    /** 唯一索引冲突即"已搬过"。Spring 通常译成 DuplicateKeyException，个别驱动/版本只到父类，兜底看 ORA-00001 */
    private static boolean isDuplicate(DataAccessException ex) {
        if (ex instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("ORA-00001")) {
                return true;
            }
        }
        return false;
    }
}
