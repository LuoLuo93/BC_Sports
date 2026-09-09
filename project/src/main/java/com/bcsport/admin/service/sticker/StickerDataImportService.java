package com.bcsport.admin.service.sticker;

import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.bcsport.admin.util.ExcelSaxUtils;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.importer.ImportLogRecorder;
import com.bcsport.admin.importer.ImportOutcome;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.service.ImportLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

/**
 * 贴纸资料批量导入：按货号(name)更新 ERP M_PRODUCT 的执行标准/EAN13/安全类别/4个材质字段。
 * <p>
 * 数据写伯俊 ERP M_PRODUCT（跨库直写），导入日志存统一日志表（R01）。
 * 语义与详情页手工编辑的差别：Excel 留空的字段不更新（保留库内原值），不写 NULL 清空，避免模板漏填误清 ERP 主数据。
 * <p>
 * 导入为两段式（读时校验收集→读后存在性检查→单事务批量 MERGE 全有或全无），
 * 与引擎的"批满即写"语义不同，故保留自有流程、只复用公共工具与统一日志（R01 决策，见方案文档 §5）。
 */
@Slf4j
@Service
public class StickerDataImportService {

    private static final int MAX_ERRORS = 100;
    private static final int MAX_ROWS = 500_000;
    /** Oracle IN 列表上限 1000，留余量取 900 */
    private static final int EXIST_BATCH_SIZE = 900;
    /** MERGE INTO 单批行数，兼顾 SQL 长度与绑定变量上限 */
    private static final int UPDATE_BATCH_SIZE = 500;

    /** 可更新字段标识（除 materialNumber 外） */
    private static final String[] VALUE_FIELDS = {
            "executionStandard", "ean13", "safetyCategory",
            "fabCode", "fabElement", "acCode", "accElement"
    };

    /** 表头别名 → 字段标识 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("货号", "materialNumber");
        HEADER_ALIAS.put("物料编号", "materialNumber");
        HEADER_ALIAS.put("materialNumber", "materialNumber");
        HEADER_ALIAS.put("执行标准", "executionStandard");
        HEADER_ALIAS.put("executionStandard", "executionStandard");
        HEADER_ALIAS.put("EAN13", "ean13");
        HEADER_ALIAS.put("ean13", "ean13");
        HEADER_ALIAS.put("安全类别", "safetyCategory");
        HEADER_ALIAS.put("安全技术类别", "safetyCategory");
        HEADER_ALIAS.put("safetyCategory", "safetyCategory");
        HEADER_ALIAS.put("面料成分1", "fabCode");
        HEADER_ALIAS.put("面料1", "fabCode");
        HEADER_ALIAS.put("fabCode", "fabCode");
        HEADER_ALIAS.put("面料成分2", "fabElement");
        HEADER_ALIAS.put("面料2", "fabElement");
        HEADER_ALIAS.put("fabElement", "fabElement");
        HEADER_ALIAS.put("辅料成分1", "acCode");
        HEADER_ALIAS.put("辅料1", "acCode");
        HEADER_ALIAS.put("acCode", "acCode");
        HEADER_ALIAS.put("辅料成分2", "accElement");
        HEADER_ALIAS.put("辅料2", "accElement");
        HEADER_ALIAS.put("accElement", "accElement");
    }

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    /** 伯俊ERP数据源事务管理器（导入整体回滚用） */
    @Autowired
    @Qualifier("bjerpTransactionManager")
    private PlatformTransactionManager bjerpTransactionManager;

    @Autowired
    private ImportLogRecorder importLogRecorder;

    @Autowired
    private ImportLogService importLogService;

    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // 0. 文件格式检测
        String realFormat = ExcelSaxUtils.detectFormat(file);
        log.info("贴纸资料导入文件真实格式: {}", realFormat);
        if (!"xlsx".equals(realFormat) && !"xls".equals(realFormat)) {
            return failFast(file, "文件不是标准的 Excel 格式（检测为 " + realFormat + "），请用 Excel 打开后另存为 .xlsx 再上传");
        }

        // 1. SAX 流式读取全部行，同时做行级校验
        // 每个 sheet 独立维护表头列位：不同 sheet 列顺序可能不同，共用一份会错位读值、把错数据写进 ERP
        Map<Integer, Map<String, Integer>> sheetColumns = new HashMap<>();
        // 有效数据（货号 -> 字段值，null=留空不更新），同一货号重复时按字段合并（后行的非空值覆盖）
        LinkedHashMap<String, Map<String, String>> validRows = new LinkedHashMap<>();
        // 重复行统计：货号 -> 出现次数
        Map<String, Integer> duplicateCount = new LinkedHashMap<>();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        int[] total = {0};

        RowHandler handler = (sheetIndex, rowIndex, rowCells) -> {
            Map<String, Integer> columnIndex = sheetColumns.computeIfAbsent(sheetIndex, k -> new HashMap<>());
            if (rowIndex == 0) {
                if (columnIndex.isEmpty() && rowCells != null) {
                    for (int i = 0; i < rowCells.size(); i++) {
                        Object h = rowCells.get(i);
                        if (h == null) continue;
                        String field = HEADER_ALIAS.get(String.valueOf(h).trim());
                        if (field != null) columnIndex.put(field, i);
                    }
                }
                return;
            }
            if (rowCells == null || rowCells.isEmpty()) return;
            // 本 sheet 表头未识别出「货号」列 → 视为非数据 sheet（说明页等），整 sheet 静默跳过
            if (!columnIndex.containsKey("materialNumber")) return;

            // 整行所有单元格值均为空 → 视为空行(Excel 常把格式刷到 1048576 行，POI 会回放大量"空单元格"行)
            // 静默跳过：不计入 total、不算失败，否则会被误报成上百万条失败
            boolean allBlank = true;
            for (Object c : rowCells) {
                if (c != null && !String.valueOf(c).trim().isEmpty()) {
                    allBlank = false;
                    break;
                }
            }
            if (allBlank) return;

            int rowNum = (int) rowIndex + 1;
            int cnt = ++total[0];
            // 超上限提示只发一次：超限后的每一行都会走到这，按 errors.size()<MAX_ERRORS 判断会把同一条消息刷近百遍
            if (cnt > MAX_ROWS) {
                if (cnt == MAX_ROWS + 1 && errors.size() < MAX_ERRORS) {
                    errors.add("数据超过 " + MAX_ROWS + " 行上限，已停止处理");
                }
                return;
            }

            String materialNumber = normalizeNumericText(ExcelSaxUtils.cellStr(rowCells, columnIndex.get("materialNumber")));
            // 校验：货号必填
            if (!StringUtils.hasText(materialNumber)) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：货号不能为空");
                return;
            }
            Map<String, String> values = new HashMap<>();
            for (String field : VALUE_FIELDS) {
                values.put(field, ExcelSaxUtils.cellStr(rowCells, columnIndex.get(field)));
            }
            // EAN13：允许空，非空必须 12 位纯数字（与详情页编辑同规则，不算校验位）
            String ean13 = normalizeNumericText(values.get("ean13"));
            values.put("ean13", ean13);
            if (ean13 != null && !ean13.matches("\\d{12}")) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：EAN13 必须为 12 位纯数字（当前值：" + ean13 + "）");
                return;
            }
            // 7 个可更新字段全为空 → 该行无事可做，跳过
            if (values.values().stream().allMatch(v -> !StringUtils.hasText(v))) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：执行标准/EAN13/安全类别/材质均为空，已跳过");
                return;
            }

            // 重复行按字段合并：后行非空值覆盖，留空保留前行值
            duplicateCount.merge(materialNumber, 1, Integer::sum);
            Map<String, String> existing = validRows.get(materialNumber);
            if (existing == null) {
                validRows.put(materialNumber, values);
            } else {
                for (String field : VALUE_FIELDS) {
                    String v = values.get(field);
                    if (StringUtils.hasText(v)) existing.put(field, v);
                }
            }
        };

        ExcelSaxUtils.readAllSheets(file, realFormat, handler, "贴纸资料导入");

        // 2. 表头缺失校验（缺货号列或没有任何可更新列时不执行任何更新，同时落 FAILED 日志留审计）
        boolean anyDataSheet = sheetColumns.values().stream()
                .anyMatch(m -> m.containsKey("materialNumber"));
        if (!anyDataSheet) {
            return failFast(file, "Excel缺少必需列：货号，请检查表头或下载导入模板");
        }
        Set<String> allColumns = new HashSet<>();
        sheetColumns.values().forEach(m -> allColumns.addAll(m.keySet()));
        boolean hasAnyValueColumn = false;
        for (String field : VALUE_FIELDS) {
            if (allColumns.contains(field)) { hasAnyValueColumn = true; break; }
        }
        if (!hasAnyValueColumn) {
            return failFast(file, "Excel缺少可更新列（执行标准/EAN13/安全类别/面料成分/辅料成分），请检查表头或下载导入模板");
        }

        // 2.5 重复行提示（按字段合并，告知用户哪些货号出现多次）
        for (Map.Entry<String, Integer> e : duplicateCount.entrySet()) {
            if (e.getValue() > 1 && errors.size() < MAX_ERRORS) {
                errors.add("货号 [" + e.getKey() + "] 出现 " + e.getValue() + " 次，已按非空值合并（后行覆盖）");
            }
        }

        // 3. 货号存在性校验：分批查询 ERP 里存在的货号，不存在的提前过滤
        //    Oracle IN 列表上限 1000，按 EXIST_BATCH_SIZE 分批避免 ORA-01795
        int notExistCount = 0;
        if (!validRows.isEmpty()) {
            Set<String> existSet = new HashSet<>();
            List<String> allNumbers = new ArrayList<>(validRows.keySet());
            for (int i = 0; i < allNumbers.size(); i += EXIST_BATCH_SIZE) {
                int end = Math.min(i + EXIST_BATCH_SIZE, allNumbers.size());
                existSet.addAll(bjerpProductMapper.selectExistMaterialNumbers(allNumbers.subList(i, end)));
            }
            // 移除不存在的货号
            Iterator<String> it = validRows.keySet().iterator();
            while (it.hasNext()) {
                String num = it.next();
                if (!existSet.contains(num)) {
                    if (errors.size() < MAX_ERRORS) errors.add("货号 [" + num + "] 在ERP中未找到，已跳过");
                    it.remove();
                    notExistCount++;
                }
            }
        }

        // 4. 在伯俊数据源事务内批量更新：MERGE INTO 分批，任意异常整体回滚，保护货品主数据
        int[] success = {0};
        if (!validRows.isEmpty()) {
            try {
                new TransactionTemplate(bjerpTransactionManager).execute(status -> {
                    List<Map<String, String>> batch = new ArrayList<>();
                    for (Map.Entry<String, Map<String, String>> e : validRows.entrySet()) {
                        Map<String, String> row = new HashMap<>(e.getValue());
                        row.put("materialNumber", e.getKey());
                        batch.add(row);
                        if (batch.size() >= UPDATE_BATCH_SIZE) {
                            success[0] += bjerpProductMapper.batchUpdateStickerFields(batch);
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        success[0] += bjerpProductMapper.batchUpdateStickerFields(batch);
                    }
                    return null;
                });
            } catch (Exception e) {
                // 事务已回滚：DB 实际未更新任何记录，success 归零，全部算失败
                log.error("贴纸资料导入事务失败，已整体回滚: {}", e.getMessage(), e);
                success[0] = 0;
                if (errors.size() < MAX_ERRORS) {
                    errors.add("导入失败已整体回滚: " + e.getMessage());
                }
            }
        }

        // M_PRODUCT.name 非唯一时 MERGE 会一次更新同名多行，affected 可能大于有效行数，失败数按 0 封底
        int fail = Math.max(0, total[0] - success[0]);
        if (fail > MAX_ERRORS && !errors.isEmpty()) {
            errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ERRORS + " 条");
        }
        log.info("贴纸资料导入完成: total={}, success={}, fail={}, notExist={}", total[0], success[0], fail, notExistCount);

        // 有合法表头但一行有效数据都没读到
        if (total[0] == 0 && errors.isEmpty()) {
            errors.add("未读取到任何数据行");
        }
        // success=0（含 total=0）都算 FAILED；只有部分失败才标 PARTIAL——非标准状态推导，走 withStatus
        String status = (success[0] == 0) ? ImportOutcome.STATUS_FAILED
                : (fail == 0 ? ImportOutcome.STATUS_SUCCESS : ImportOutcome.STATUS_PARTIAL);
        // 兜底：有失败但 errors 为空，补一条说明
        if (fail > 0 && errors.isEmpty()) {
            errors.add("共 " + fail + " 条数据未导入（可能因必填字段为空、数据类型不匹配或数据库约束冲突），请检查源数据");
        }
        ImportOutcome outcome = ImportOutcome.withStatus(total[0], success[0], fail, errors, status);
        importLogRecorder.record(ImportType.STICKER_DATA, outcome, file);
        return outcome.toResultMap();
    }

    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.STICKER_DATA, pageQuery);
    }

    // ==================== 工具方法 ====================

    /** 导入未执行任何更新即终止（伪Excel/缺表头等）：同样落 FAILED 日志，保证审计无断档 */
    private Map<String, Object> failFast(MultipartFile file, String error) {
        ImportOutcome rejected = ImportOutcome.rejected(error);
        importLogRecorder.record(ImportType.STICKER_DATA, rejected, file);
        return rejected.toResultMap();
    }

    /**
     * 还原数值单元格被读成的伪数字文本：xls 数值单元格可能读成 "1.23456789012E11" 或 "123.0"，
     * 货号/EAN13 是纯数字串时去掉科学计数与尾部 ".0"；非纯数字文本原样返回。
     */
    private String normalizeNumericText(String raw) {
        if (raw == null) return null;
        String s = raw.replace(" ", "").replace(",", "");
        if (s.matches("^\\d+(\\.0+)?$")) {
            return s.split("\\.")[0];
        }
        if (s.matches("(?i)^\\d(\\.\\d+)?e\\+?\\d+$")) {
            return new BigDecimal(s).toPlainString();
        }
        return raw.trim();
    }
}
