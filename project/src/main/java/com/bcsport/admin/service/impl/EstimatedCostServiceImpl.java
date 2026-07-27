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
import org.springframework.stereotype.Service;
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
            throw new BusinessException("物料编号不能为空");
        }
        int rows = bjerpProductMapper.updatePrecost(materialNumber, precost);
        if (rows == 0) {
            throw new BusinessException("物料编号不存在: " + materialNumber);
        }
        log.info("预估成本已更新: materialNumber={}, precost={}, rows={}", materialNumber, precost, rows);
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

        int[] total = {0};
        int[] success = {0};
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        Map<String, Integer> columnIndex = new HashMap<>();

        RowHandler handler = (sheetIndex, rowIndex, rowCells) -> {
            if (rowIndex == 0) {
                // 解析表头
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

            // 校验必填：物料编号
            if (!StringUtils.hasText(materialNumber)) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：物料编号不能为空");
                return;
            }
            // 预估成本为空也允许（清空值），但记录提示
            if (!StringUtils.hasText(precost)) {
                precost = "";
            }

            try {
                int rows = bjerpProductMapper.updatePrecost(materialNumber, precost);
                if (rows == 0) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：物料编号 [" + materialNumber + "] 不存在");
                } else {
                    success[0]++;
                }
            } catch (Exception e) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：更新异常 - " + e.getMessage());
            }
        };

        readAllSheets(file, realFormat, handler);

        // 表头缺失校验
        List<String> missingHeaders = new ArrayList<>();
        if (!columnIndex.containsKey("materialNumber")) missingHeaders.add("货号");
        if (!columnIndex.containsKey("precost")) missingHeaders.add("预估成本");
        if (!missingHeaders.isEmpty()) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "Excel缺少必需列：" + String.join("、", missingHeaders) + "，请检查表头或下载导入模板"));
        }

        int fail = total[0] - success[0];
        if (fail > MAX_ERRORS && !errors.isEmpty()) {
            errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ERRORS + " 条");
        }
        log.info("预估成本导入完成: total={}, success={}, fail={}", total[0], success[0], fail);

        String status = (total[0] == 0) ? "FAILED" : (fail == 0 ? "SUCCESS" : "PARTIAL");
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
