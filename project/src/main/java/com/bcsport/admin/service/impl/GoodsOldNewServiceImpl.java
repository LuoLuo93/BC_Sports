package com.bcsport.admin.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.GoodsOldNew;
import com.bcsport.admin.entity.bi.GoodsImportLog;
import com.bcsport.admin.mapper.GoodsOldNewMapper;
import com.bcsport.admin.mapper.GoodsImportLogMapper;
import com.bcsport.admin.service.GoodsOldNewService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 货品资料导入实现
 * SAX 流式读取 + 批量 MERGE 入库
 */
@Slf4j
@Service
public class GoodsOldNewServiceImpl implements GoodsOldNewService {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_ERRORS = 100;
    private static final int MAX_ROWS = 2_000_000;

    /** 表头别名 → 字段标识 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("品牌", "brand");
        HEADER_ALIAS.put("brand", "brand");
        HEADER_ALIAS.put("Brand", "brand");
        HEADER_ALIAS.put("货号", "articleNo");
        HEADER_ALIAS.put("article_no", "articleNo");
        HEADER_ALIAS.put("articleno", "articleNo");
        HEADER_ALIAS.put("产品季", "season");
        HEADER_ALIAS.put("season", "season");
        HEADER_ALIAS.put("Season", "season");
        HEADER_ALIAS.put("货品分类", "category");
        HEADER_ALIAS.put("category", "category");
        HEADER_ALIAS.put("Category", "category");
    }

    @Autowired
    private GoodsOldNewMapper goodsOldNewMapper;

    @Autowired
    private GoodsImportLogMapper importLogMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public PageResult<GoodsOldNew> page(PageQuery pageQuery, String brand, String articleNo) {
        Page<GoodsOldNew> page = pageQuery.toPage();
        LambdaQueryWrapper<GoodsOldNew> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(brand)) {
            wrapper.like(GoodsOldNew::getBrand, brand);
        }
        if (StringUtils.hasText(articleNo)) {
            wrapper.like(GoodsOldNew::getArticleNo, articleNo);
        }
        wrapper.orderByDesc(GoodsOldNew::getId);
        Page<GoodsOldNew> result = goodsOldNewMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // 0. 文件格式检测
        String realFormat = detectFormat(file);
        log.info("GoodsOldNew 导入文件真实格式: {}", realFormat);
        if (!"xlsx".equals(realFormat) && !"xls".equals(realFormat)) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "文件不是标准的 Excel 格式（检测为 " + realFormat + "），请用 Excel 打开后另存为 .xlsx 再上传"));
        }

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        Map<String, Integer> columnIndex = new HashMap<>();
        // 文件内去重：brand+articleNo+season 组合键
        Set<String> batchKeys = new HashSet<>();
        List<GoodsOldNew> buffer = new ArrayList<>(BATCH_SIZE);

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
            int cnt = total.incrementAndGet();
            if (cnt > MAX_ROWS) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add("数据超过 " + MAX_ROWS + " 行上限，已停止处理");
                }
                return;
            }

            try {
                GoodsOldNew entity = mapRow(rowCells, columnIndex);
                // 校验必填
                if (!StringUtils.hasText(entity.getBrand())) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：品牌不能为空");
                    return;
                }
                if (!StringUtils.hasText(entity.getArticleNo())) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：货号不能为空");
                    return;
                }
                if (!StringUtils.hasText(entity.getSeason())) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：产品季不能为空");
                    return;
                }

                String key = entity.getBrand() + "|" + entity.getArticleNo() + "|" + entity.getSeason();
                if (batchKeys.contains(key)) {
                    buffer.removeIf(e -> key.equals(e.getBrand() + "|" + e.getArticleNo() + "|" + e.getSeason()));
                } else {
                    batchKeys.add(key);
                }
                buffer.add(entity);

                if (buffer.size() >= BATCH_SIZE) {
                    flushBatch(buffer, batchKeys, success, errors);
                }
            } catch (Exception e) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：解析异常 - " + e.getMessage());
            }
        };

        readAllSheets(file, realFormat, handler);

        // 表头缺失校验
        List<String> missingHeaders = new ArrayList<>();
        if (!columnIndex.containsKey("brand")) missingHeaders.add("品牌");
        if (!columnIndex.containsKey("articleNo")) missingHeaders.add("货号");
        if (!columnIndex.containsKey("season")) missingHeaders.add("产品季");
        if (!columnIndex.containsKey("category")) missingHeaders.add("货品分类");
        if (!missingHeaders.isEmpty()) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "Excel缺少必需列：" + String.join("、", missingHeaders) + "，请检查表头"));
        }

        if (!buffer.isEmpty()) {
            flushBatch(buffer, batchKeys, success, errors);
        }

        int fail = total.get() - success.get();
        if (fail > MAX_ERRORS && !errors.isEmpty()) {
            errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ERRORS + " 条");
        }
        log.info("GoodsOldNew 导入完成: total={}, success={}, fail={}", total.get(), success.get(), fail);

        String status = (total.get() == 0) ? "FAILED" : (fail == 0 ? "SUCCESS" : "PARTIAL");
        saveImportLog(file, total.get(), success.get(), fail, status, errors);

        return buildResult(total.get(), success.get(), fail, errors);
    }

    @Override
    public PageResult<GoodsImportLog> logPage(PageQuery pageQuery) {
        Page<GoodsImportLog> page = importLogMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<GoodsImportLog>().orderByDesc(GoodsImportLog::getId));
        return PageResult.of(page);
    }

    private void flushBatch(List<GoodsOldNew> buffer, Set<String> batchKeys,
                            AtomicInteger success, List<String> errors) {
        if (buffer.isEmpty()) return;
        List<GoodsOldNew> toWrite = new ArrayList<>(buffer);
        buffer.clear();
        batchKeys.clear();
        String currentUser = ShiroSecurityUtils.getCurrentUsername();
        toWrite.forEach(e -> {
            e.setCreateBy(currentUser);
            e.setUpdateBy(currentUser);
        });
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        try {
            txTemplate.execute(status -> {
                goodsOldNewMapper.mergeBatch(toWrite);
                return null;
            });
            success.addAndGet(toWrite.size());
        } catch (Exception e) {
            log.error("GoodsOldNew 批量入库失败", e);
            if (errors.size() < MAX_ERRORS) errors.add("批量入库失败: " + e.getMessage());
        }
    }

    private GoodsOldNew mapRow(List<Object> cells, Map<String, Integer> columnIndex) {
        GoodsOldNew e = new GoodsOldNew();
        boolean useHeader = !columnIndex.isEmpty();
        e.setBrand(cellStr(cells, useHeader ? columnIndex.get("brand") : 0));
        e.setArticleNo(cellStr(cells, useHeader ? columnIndex.get("articleNo") : 1));
        e.setSeason(cellStr(cells, useHeader ? columnIndex.get("season") : 2));
        e.setCategory(cellStr(cells, useHeader ? columnIndex.get("category") : 3));
        return e;
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
                log.info("GoodsOldNew xlsx 共 {} 个 sheet", sheetCount);
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
            GoodsImportLog logEntity = new GoodsImportLog();
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
            log.warn("保存导入日志失败: {}", e.getMessage());
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
