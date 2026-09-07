package com.bcsport.admin.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.OdsSalesMainMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.bi.DwSalesImportLog;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import com.bcsport.admin.mapper.DwSalesImportLogMapper;
import com.bcsport.admin.service.OdsSalesMainService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数仓销售查看实现（查询/编辑走 bidw 数据源；期初导入 SAX 流式 + 分批纯INSERT + 主库导入日志）
 * 参考 SalesBudgetFillDailyServiceImpl 的大文件导入骨架
 */
@Slf4j
@Service
public class OdsSalesMainServiceImpl implements OdsSalesMainService {

    static {
        // 放宽 POI zip炸弹防护的压缩比阈值：150W行级 xlsx 的 sheet XML 高度重复，
        // 压缩比可达 3e-7 量级，低于默认 0.01 会被误判为攻击(内部受控上传，安全)
        org.apache.poi.openxml4j.util.ZipSecureFile.setMinInflateRatio(1e-9);
    }

    private static final int BATCH_SIZE = 1000;
    private static final int MAX_ERRORS = 100;
    private static final int MAX_ROWS = 3_000_000;
    /** 序列分块取号大小：一次取1万个，避免150W行逐行NEXTVAL的百万次往返 */
    private static final int ID_BLOCK = 10000;

    /** 表头(英文列名,大写) -> 实体字段名；BILL_ID/ITEM_ID 文件里有值则用、空则自动生成 */
    private static final Map<String, String> HEADER_FIELD = new HashMap<>();
    static {
        HEADER_FIELD.put("BILL_NO", "billNo");
        HEADER_FIELD.put("BILL_DATE", "billDate");
        HEADER_FIELD.put("BILL_TIME", "billTime");
        HEADER_FIELD.put("VIP_CODE", "vipCode");
        HEADER_FIELD.put("VIP_MOBILE", "vipMobile");
        HEADER_FIELD.put("OMS_SOURCECODE", "omsSourcecode");
        HEADER_FIELD.put("SALES_TYPE", "salesType");
        HEADER_FIELD.put("BILL_ID", "billId");
        HEADER_FIELD.put("ITEM_ID", "itemId");
        HEADER_FIELD.put("BILL_POS_ID", "billPosId");
        HEADER_FIELD.put("BILL_POS_CODE", "billPosCode");
        HEADER_FIELD.put("BILL_POS_NAME", "billPosName");
        HEADER_FIELD.put("STORE_ID", "storeId");
        HEADER_FIELD.put("STORE_CODE", "storeCode");
        HEADER_FIELD.put("STORE_NAME", "storeName");
        HEADER_FIELD.put("PRODUCT_CODE", "productCode");
        HEADER_FIELD.put("PRODUCT_STYLE_NO", "productStyleNo");
        HEADER_FIELD.put("PRODUCT_NAME", "productName");
        HEADER_FIELD.put("COLORSALIAS", "colorsalias");
        HEADER_FIELD.put("BARCODE", "barcode");
        HEADER_FIELD.put("SIZES", "sizes");
        HEADER_FIELD.put("QTY", "qty");
        HEADER_FIELD.put("RETAIL_PRICE", "retailPrice");
        HEADER_FIELD.put("RETAIL_AMOUNT", "retailAmount");
        HEADER_FIELD.put("TRANSACTION_AMOUNT", "transactionAmount");
        HEADER_FIELD.put("REVENUE", "revenue");
        HEADER_FIELD.put("RECALC_REVENUE", "recalcRevenue");
        HEADER_FIELD.put("NEW_OLD_NAME_ADJUST", "newOldNameAdjust");
        HEADER_FIELD.put("ANCHOR_SUMMARYID", "anchorSummaryid");
        HEADER_FIELD.put("ANCHOR_SUMMARYNAME", "anchorSummaryname");
        HEADER_FIELD.put("PROMOTION_NAME", "promotionName");
    }

    @Autowired
    private OdsSalesMainMapper odsSalesMainMapper;

    @Autowired
    private DwSalesImportLogMapper importLogMapper;

    /** bidw 数据源事务管理器（分批 INSERT 入库用） */
    @Autowired
    @Qualifier("bidwTransactionManager")
    private PlatformTransactionManager bidwTransactionManager;

    @Override
    public PageResult<OdsSalesMain> page(PageQuery pageQuery, OdsSalesMainQueryDTO queryDTO) {
        Page<OdsSalesMain> result = odsSalesMainMapper.selectPage(pageQuery.toPage(), queryDTO);
        return PageResult.of(result);
    }

    @Override
    public boolean update(OdsSalesMainUpdateDTO dto) {
        return odsSalesMainMapper.updateRow(dto) > 0;
    }

    @Override
    public PageResult<DwSalesImportLog> logPage(PageQuery pageQuery) {
        Page<DwSalesImportLog> page = importLogMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<DwSalesImportLog>().orderByDesc(DwSalesImportLog::getId));
        return PageResult.of(page);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // 0. 文件真实格式检测(防伪Excel)
        String realFormat = detectFormat(file);
        log.info("数仓销售导入 文件真实格式: {}", realFormat);
        if (!"xlsx".equals(realFormat) && !"xls".equals(realFormat)) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "文件不是标准的 Excel 格式（检测为 " + realFormat + "），请用 Excel 打开后另存为 .xlsx 再上传"));
        }

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        // 每 sheet 独立表头(150W行必然多sheet)；同一单据号共用一个BILL_ID
        ImportContext ctx = new ImportContext(errors);
        RowHandler handler = (sheetIndex, rowIndex, rowCells) -> {
            if (rowCells == null || rowCells.isEmpty()) return;

            // 表头行判定：≥5个单元格命中已知列名即视为表头(每个sheet需含表头行)
            if (ctx.isHeaderRow(rowCells)) {
                ctx.parseHeader(rowCells);
                return;
            }
            if (ctx.columnIndex.isEmpty()) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add("sheet" + (sheetIndex + 1) + "第" + (rowIndex + 1) + "行：未识别到表头，已跳过(每个sheet第一行需为表头)");
                }
                return;
            }

            int rowNum = total.incrementAndGet();
            if (rowNum > MAX_ROWS) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add("数据超过 " + MAX_ROWS + " 行上限，已停止处理");
                }
                return;
            }

            try {
                OdsSalesMain row = mapRow(rowCells, ctx);
                // 必填校验：单据号(自然键)、提交时间(查询主线索)
                if (!StringUtils.hasText(row.getBillNo())) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：单据号(BILL_NO)不能为空");
                    return;
                }
                if (row.getBillTime() == null) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：提交时间(BILL_TIME)为空或格式错误，应为 yyyy-MM-dd HH:mm:ss");
                    return;
                }
                // ID补全：ITEM_ID每行一号；BILL_ID按单据号分组共号(文件带ID则优先用文件的)
                if (row.getItemId() == null) row.setItemId(ctx.itemIds.next());
                row.setBillId(ctx.billIdOf(row.getBillNo(), row.getBillId()));
                ctx.buffer.add(row);
                if (ctx.buffer.size() >= BATCH_SIZE) {
                    ctx.flush(success);
                }
            } catch (Exception e) {
                if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：解析异常 - " + e.getMessage());
            }
        };

        readAllSheets(file, realFormat, handler);

        // 表头缺失校验(一个sheet都没识别到表头=整个文件结构不对)
        if (ctx.headerSeenCount == 0) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "未识别到任何表头行：表头需为英文列名(BILL_NO/BILL_TIME/...)且首个sheet第一行为表头"));
        }

        if (!ctx.buffer.isEmpty()) {
            ctx.flush(success);
        }

        int fail = total.get() - success.get();
        if (fail > MAX_ERRORS && !errors.isEmpty()) {
            errors.add("...共 " + fail + " 条未导入，仅显示前 " + MAX_ERRORS + " 条");
        }
        log.info("数仓销售导入完成: total={}, success={}, fail={}", total.get(), success.get(), fail);

        String status = (total.get() == 0) ? "FAILED" : (fail == 0 ? "SUCCESS" : "PARTIAL");
        if (fail > 0 && errors.isEmpty()) {
            errors.add("共 " + fail + " 条数据未导入（可能因必填字段为空、数据类型不匹配或数据库约束冲突），请检查源数据");
        }
        saveImportLog(file, total.get(), success.get(), fail, status, errors);

        return buildResult(total.get(), success.get(), fail, errors);
    }

    // ===== 导入上下文：表头/缓冲/取号器 =====

    private class ImportContext {
        final Map<String, Integer> columnIndex = new HashMap<>();
        final List<OdsSalesMain> buffer = new ArrayList<>(BATCH_SIZE);
        final List<String> errors;
        /** 单据号 -> BILL_ID(同一单据共号) */
        final Map<String, Long> billIdMap = new HashMap<>();
        final IdAllocator itemIds = new IdAllocator(true);
        final IdAllocator billIds = new IdAllocator(false);
        int headerSeenCount = 0;

        ImportContext(List<String> errors) {
            this.errors = errors;
        }

        boolean isHeaderRow(List<Object> cells) {
            int hits = 0;
            for (Object c : cells) {
                if (c == null) continue;
                if (HEADER_FIELD.containsKey(String.valueOf(c).trim().toUpperCase())) hits++;
                if (hits >= 5) return true;
            }
            return false;
        }

        void parseHeader(List<Object> cells) {
            columnIndex.clear();
            for (int i = 0; i < cells.size(); i++) {
                Object h = cells.get(i);
                if (h == null) continue;
                String field = HEADER_FIELD.get(String.valueOf(h).trim().toUpperCase());
                if (field != null) columnIndex.put(field, i);
            }
            headerSeenCount++;
        }

        Long billIdOf(String billNo, Long fileProvided) {
            if (fileProvided != null) {
                return billIdMap.computeIfAbsent(billNo, k -> fileProvided);
            }
            return billIdMap.computeIfAbsent(billNo, k -> billIds.next());
        }

        void flush(AtomicInteger success) {
            if (buffer.isEmpty()) return;
            List<OdsSalesMain> toWrite = new ArrayList<>(buffer);
            buffer.clear();
            TransactionTemplate txTemplate = new TransactionTemplate(bidwTransactionManager);
            try {
                txTemplate.execute(status -> {
                    odsSalesMainMapper.insertBatch(toWrite);
                    return null;
                });
                success.addAndGet(toWrite.size());
            } catch (Exception e) {
                log.error("数仓销售导入 批量入库失败", e);
                if (errors.size() < MAX_ERRORS) {
                    String reason = e.getMessage() == null ? "未知错误" : e.getMessage();
                    for (OdsSalesMain item : toWrite) {
                        if (errors.size() >= MAX_ERRORS) break;
                        errors.add("入库失败 [单据号=" + item.getBillNo()
                                + ", 货号=" + item.getProductCode()
                                + "]: " + reason);
                    }
                }
            }
        }
    }

    /** 序列分块取号器：本地池空了再批量取 ID_BLOCK 个 */
    private class IdAllocator {
        private final boolean item;
        private final Deque<Long> pool = new ArrayDeque<>();

        IdAllocator(boolean item) {
            this.item = item;
        }

        long next() {
            if (pool.isEmpty()) {
                List<Long> block = item
                        ? odsSalesMainMapper.drawItemIds(ID_BLOCK)
                        : odsSalesMainMapper.drawBillIds(ID_BLOCK);
                if (block == null || block.isEmpty()) {
                    throw new IllegalStateException("序列取号失败(item=" + item + ")，请确认 BI_DW 序列已创建");
                }
                pool.addAll(block);
            }
            return pool.pop();
        }
    }

    // ===== 行映射与解析 =====

    private OdsSalesMain mapRow(List<Object> cells, ImportContext ctx) {
        OdsSalesMain e = new OdsSalesMain();
        Map<String, Integer> idx = ctx.columnIndex;
        e.setBillId(parseLongOrNull(cellStr(cells, idx.get("billId"))));
        e.setBillNo(cellStr(cells, idx.get("billNo")));
        e.setBillDate(parseBillDate(cellStr(cells, idx.get("billDate"))));
        e.setBillTime(parseTime(cellStr(cells, idx.get("billTime"))));
        e.setVipCode(cellStr(cells, idx.get("vipCode")));
        e.setVipMobile(cellStr(cells, idx.get("vipMobile")));
        e.setOmsSourcecode(cellStr(cells, idx.get("omsSourcecode")));
        e.setSalesType(cellStr(cells, idx.get("salesType")));
        e.setItemId(parseLongOrNull(cellStr(cells, idx.get("itemId"))));
        e.setBillPosId(parseLongOrNull(cellStr(cells, idx.get("billPosId"))));
        e.setBillPosCode(cellStr(cells, idx.get("billPosCode")));
        e.setBillPosName(cellStr(cells, idx.get("billPosName")));
        e.setStoreId(parseLongOrNull(cellStr(cells, idx.get("storeId"))));
        e.setStoreCode(cellStr(cells, idx.get("storeCode")));
        e.setStoreName(cellStr(cells, idx.get("storeName")));
        e.setProductCode(cellStr(cells, idx.get("productCode")));
        e.setProductStyleNo(cellStr(cells, idx.get("productStyleNo")));
        e.setProductName(cellStr(cells, idx.get("productName")));
        e.setColorsalias(cellStr(cells, idx.get("colorsalias")));
        e.setBarcode(cellStr(cells, idx.get("barcode")));
        e.setSizes(cellStr(cells, idx.get("sizes")));
        e.setQty(parseBigDecimal(cellStr(cells, idx.get("qty"))));
        e.setRetailPrice(parseBigDecimal(cellStr(cells, idx.get("retailPrice"))));
        e.setRetailAmount(parseBigDecimal(cellStr(cells, idx.get("retailAmount"))));
        e.setTransactionAmount(parseBigDecimal(cellStr(cells, idx.get("transactionAmount"))));
        e.setRevenue(parseBigDecimal(cellStr(cells, idx.get("revenue"))));
        e.setRecalcRevenue(parseBigDecimal(cellStr(cells, idx.get("recalcRevenue"))));
        e.setNewOldNameAdjust(cellStr(cells, idx.get("newOldNameAdjust")));
        e.setAnchorSummaryid(cellStr(cells, idx.get("anchorSummaryid")));
        e.setAnchorSummaryname(cellStr(cells, idx.get("anchorSummaryname")));
        e.setPromotionName(cellStr(cells, idx.get("promotionName")));
        return e;
    }

    private String cellStr(List<Object> cells, Integer idx) {
        if (idx == null || idx < 0 || idx >= cells.size()) return null;
        Object v = cells.get(idx);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /** BILL_TIME：yyyy-MM-dd HH:mm:ss 为主，兼容 / 分隔与纯日期 */
    private java.util.Date parseTime(String s) {
        if (!StringUtils.hasText(s)) return null;
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd"};
        for (String p : patterns) {
            try {
                return new java.text.SimpleDateFormat(p).parse(s);
            } catch (java.text.ParseException ignored) {
            }
        }
        return null;
    }

    /** BILL_DATE：YYYYMMDD 数字(兼容Excel数值单元格的科学计数/小数尾巴) */
    private Long parseBillDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return new BigDecimal(s.replace(",", "").trim()).longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return Long.parseLong(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return new BigDecimal(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void readAllSheets(MultipartFile file, String format, RowHandler handler) throws Exception {
        if ("xlsx".equals(format)) {
            org.apache.poi.openxml4j.opc.OPCPackage pkg = org.apache.poi.openxml4j.opc.OPCPackage.open(file.getInputStream());
            try {
                int sheetCount = pkg.getPartsByName(java.util.regex.Pattern.compile("/xl/worksheets/.*\\.xml")).size();
                log.info("数仓销售导入 xlsx 共 {} 个 sheet", sheetCount);
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
            DwSalesImportLog logEntity = new DwSalesImportLog();
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
            log.warn("保存数仓销售导入日志失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildResult(int total, int success, int fail, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }
}
