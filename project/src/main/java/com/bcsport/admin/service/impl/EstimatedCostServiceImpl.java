package com.bcsport.admin.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.EstimatedCostImportLog;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.mapper.EstimatedCostImportLogMapper;
import com.bcsport.admin.service.EstimatedCostService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 预估成本管理实现
 * <p>
 * 数据存伯俊 ERP M_PRODUCT.PRECOST（跨库直读直写），导入日志存本地 Oracle 主库。
 */
@Slf4j
@Service
public class EstimatedCostServiceImpl implements EstimatedCostService {

    private static final int MAX_ERRORS = 100;
    private static final int MAX_ROWS = 500_000;

    /** 表头别名 → 字段标识 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("货号", "materialNumber");
        HEADER_ALIAS.put("物料编号", "materialNumber");
        HEADER_ALIAS.put("materialNumber", "materialNumber");
        HEADER_ALIAS.put("预估成本", "precost");
        HEADER_ALIAS.put("precost", "precost");
        HEADER_ALIAS.put("PRECOST", "precost");
    }

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    @Autowired
    private EstimatedCostImportLogMapper importLogMapper;

    /** 伯俊ERP数据源事务管理器（导入整体回滚用） */
    @Autowired
    @Qualifier("bjerpTransactionManager")
    private PlatformTransactionManager bjerpTransactionManager;

    @Override
    public PageResult<Map<String, Object>> page(PageQuery pageQuery, String materialNumber, String styleNumber, String materialName) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 20, 500), 1);
        int pageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        int offset = (pageNum - 1) * pageSize;

        String mn = escapeLike(materialNumber), sn = escapeLike(styleNumber), mname = escapeLike(materialName);
        long total = bjerpProductMapper.countEstimatedCost(mn, sn, mname);
        List<Map<String, Object>> records = total > 0
                ? bjerpProductMapper.searchEstimatedCost(mn, sn, mname, offset, pageSize)
                : Collections.emptyList();

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setRecords(records);
        result.setPages((long) Math.ceil((double) total / pageSize));
        result.setHasPrevious(pageNum > 1);
        result.setHasNext((long) pageNum < result.getPages());
        return result;
    }

    @Override
    public void updatePrecost(String materialNumber, String precost) {
        if (!StringUtils.hasText(materialNumber)) {
            throw new BusinessException("货号不能为空");
        }
        validatePrecost(precost);
        int rows = bjerpProductMapper.updatePrecost(materialNumber, precost);
        if (rows == 0) {
            throw new BusinessException("货号不存在: " + materialNumber);
        }
        log.info("预估成本已更新: materialNumber={}, precost={}, rows={}", materialNumber, precost, rows);
    }

    /**
     * 校验预估成本：允许空（清空值），非空则必须是合法数字（整数或小数，可为负）。
     */
    private void validatePrecost(String precost) {
        if (!StringUtils.hasText(precost)) return;
        String trimmed = precost.trim();
        if (!trimmed.matches("-?\\d+(\\.\\d+)?")) {
            throw new BusinessException("预估成本必须是数字: " + precost);
        }
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // 0. 文件格式检测
        String realFormat = detectFormat(file);
        log.info("预估成本导入文件真实格式: {}", realFormat);
        if (!"xlsx".equals(realFormat) && !"xls".equals(realFormat)) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "文件不是标准的 Excel 格式（检测为 " + realFormat + "），请用 Excel 打开后另存为 .xlsx 再上传"));
        }

        // 1. SAX 流式读取全部行到内存（仅货号+预估成本两列，占用极小），同时做行级校验
        Map<String, Integer> columnIndex = new HashMap<>();
        // 有效数据（货号 -> 预估成本），同一货号后行覆盖前行
        LinkedHashMap<String, String> validRows = new LinkedHashMap<>();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        int[] total = {0};

        RowHandler handler = (sheetIndex, rowIndex, rowCells) -> {
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

            int rowNum = (int) rowIndex + 1;
            int cnt = ++total[0];
            if (cnt > MAX_ROWS) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add("数据超过 " + MAX_ROWS + " 行上限，已停止处理");
                }
                return;
            }

            String materialNumber = cellStr(rowCells, columnIndex.get("materialNumber"));
            String precost = cellStr(rowCells, columnIndex.get("precost"));

            // 校验：货号必填
            if (!StringUtils.hasText(materialNumber)) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：货号不能为空");
                return;
            }
            // 预估成本为空：跳过该行（不写 NULL，避免误清空主数据）
            if (!StringUtils.hasText(precost)) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：预估成本为空，已跳过");
                return;
            }
            // 校验：预估成本必须是数字
            if (!precost.trim().matches("-?\\d+(\\.\\d+)?")) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：预估成本必须是数字（当前值：" + precost + "）");
                return;
            }
            validRows.put(materialNumber, precost);
        };

        readAllSheets(file, realFormat, handler);

        // 2. 表头缺失校验（缺列直接返回，不执行任何更新）
        List<String> missingHeaders = new ArrayList<>();
        if (!columnIndex.containsKey("materialNumber")) missingHeaders.add("货号");
        if (!columnIndex.containsKey("precost")) missingHeaders.add("预估成本");
        if (!missingHeaders.isEmpty()) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "Excel缺少必需列：" + String.join("、", missingHeaders) + "，请检查表头或下载导入模板"));
        }

        // 3. 在伯俊数据源事务内批量更新：任意异常整体回滚，保护货品主数据
        int[] success = {0};
        if (!validRows.isEmpty()) {
            try {
                new TransactionTemplate(bjerpTransactionManager).execute(status -> {
                    for (Map.Entry<String, String> e : validRows.entrySet()) {
                        int rows = bjerpProductMapper.updatePrecost(e.getKey(), e.getValue());
                        if (rows == 0) {
                            if (errors.size() < MAX_ERRORS) errors.add("货号 [" + e.getKey() + "] 不存在");
                        } else {
                            success[0]++;
                        }
                    }
                    return null;
                });
            } catch (Exception e) {
                // 事务已回滚：DB 实际未更新任何记录，success 归零，全部算失败
                log.error("预估成本导入事务失败，已整体回滚: {}", e.getMessage(), e);
                success[0] = 0;
                if (errors.size() < MAX_ERRORS) {
                    errors.add("导入失败已整体回滚: " + e.getMessage());
                }
            }
        }

        int fail = total[0] - success[0];
        if (fail > MAX_ERRORS && !errors.isEmpty()) {
            errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ERRORS + " 条");
        }
        log.info("预估成本导入完成: total={}, success={}, fail={}", total[0], success[0], fail);

        String status = (total[0] == 0) ? "FAILED" : (fail == 0 ? "SUCCESS" : "PARTIAL");
        // 兜底：有失败但 errors 为空，补一条说明
        if (fail > 0 && errors.isEmpty()) {
            errors.add("共 " + fail + " 条数据未导入（可能因必填字段为空、数据类型不匹配或数据库约束冲突），请检查源数据");
        }
        saveImportLog(file, total[0], success[0], fail, status, errors);

        return buildResult(total[0], success[0], fail, errors);
    }

    @Override
    public PageResult<EstimatedCostImportLog> logPage(PageQuery pageQuery) {
        Page<EstimatedCostImportLog> page = importLogMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<EstimatedCostImportLog>().orderByDesc(EstimatedCostImportLog::getId));
        return PageResult.of(page);
    }

    // ==================== 工具方法 ====================

    private String escapeLike(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String cellStr(List<Object> cells, Integer idx) {
        if (idx == null || idx < 0 || idx >= cells.size()) return null;
        Object v = cells.get(idx);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private void readAllSheets(MultipartFile file, String format, RowHandler handler) throws Exception {
        if ("xlsx".equals(format)) {
            org.apache.poi.openxml4j.opc.OPCPackage pkg = org.apache.poi.openxml4j.opc.OPCPackage.open(file.getInputStream());
            try {
                int sheetCount = pkg.getPartsByName(java.util.regex.Pattern.compile("/xl/worksheets/.*\\.xml")).size();
                log.info("预估成本 xlsx 共 {} 个 sheet", sheetCount);
                cn.hutool.poi.excel.sax.Excel07SaxReader saxReader = new cn.hutool.poi.excel.sax.Excel07SaxReader(handler);
                for (int s = 0; s < sheetCount; s++) {
                    saxReader.read(pkg, s);
                }
            } finally {
                pkg.revert();
            }
        } else {
            for (int s = 0; s < 20; s++) {
                try {
                    ExcelUtil.readBySax(file.getInputStream(), s, handler);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    private String detectFormat(MultipartFile file) throws Exception {
        byte[] head = new byte[8];
        try (java.io.InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) return "unknown(空文件)";
        }
        if ((head[0] & 0xFF) == 0x50 && (head[1] & 0xFF) == 0x4B) return "xlsx";
        if ((head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0) return "xls";
        String preview = new String(head, java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        String lower = preview.toLowerCase();
        if (lower.startsWith("<") || preview.contains("<table") || preview.contains("<html")
                || preview.contains("<?xml")) return "HTML/XML（伪Excel）";
        if (lower.contains(",") || lower.contains("\t") || lower.contains(";")) return "CSV/文本（伪Excel）";
        return "未知格式";
    }

    private void saveImportLog(MultipartFile file, int total, int success, int fail, String status, List<String> errors) {
        try {
            EstimatedCostImportLog logEntity = new EstimatedCostImportLog();
            logEntity.setFileName(file.getOriginalFilename());
            logEntity.setFileSize(file.getSize());
            logEntity.setTotalCount(total);
            logEntity.setSuccessCount(success);
            logEntity.setFailCount(fail);
            logEntity.setStatus(status);
            if (!errors.isEmpty()) {
                String msg = String.join("\n", errors);
                logEntity.setErrorMsg(msg.length() > 4000 ? msg.substring(0, 4000) : msg);
            }
            logEntity.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
            logEntity.setCreateTime(LocalDateTime.now());
            importLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("保存预估成本导入日志失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildResult(int total, int success, int fail, List<String> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }
}
