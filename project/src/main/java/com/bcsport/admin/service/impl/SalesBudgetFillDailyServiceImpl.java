package com.bcsport.admin.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.SalesBudgetFillDailyMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SalesBudgetQueryDTO;
import com.bcsport.admin.entity.bi.BudgetImportLog;
import com.bcsport.admin.entity.bi.SalesBudgetFillDaily;
import com.bcsport.admin.mapper.BudgetImportLogMapper;
import com.bcsport.admin.service.SalesBudgetFillDailyService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.util.Calendar;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 店铺日预算实现
 * SAX 流式读取 + 批量 MERGE 入库(BI_DW) + 导入日志(BC_SPORTS)
 * extends ServiceImpl<SalesBudgetFillDailyMapper, SalesBudgetFillDaily>
 *   → baseMapper 自动绑定 SalesBudgetFillDailyMapper(走 bidw 数据源)
 */
@Slf4j
@Service
public class SalesBudgetFillDailyServiceImpl
        extends ServiceImpl<SalesBudgetFillDailyMapper, SalesBudgetFillDaily>
        implements SalesBudgetFillDailyService {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_ERRORS = 100;
    private static final int MAX_ROWS = 2_000_000;

    /** 表头别名 → 字段标识 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("一级地区", "regionLevel1");
        HEADER_ALIAS.put("一级组织", "regionLevel1");   // 兼容旧模板
        HEADER_ALIAS.put("region_level1", "regionLevel1");
        HEADER_ALIAS.put("department_1", "regionLevel1"); // 兼容旧列名
        HEADER_ALIAS.put("二级地区", "regionLevel2");
        HEADER_ALIAS.put("二级组织", "regionLevel2");   // 兼容旧模板
        HEADER_ALIAS.put("region_level2", "regionLevel2");
        HEADER_ALIAS.put("department_2", "regionLevel2"); // 兼容旧列名
        HEADER_ALIAS.put("渠道类型", "channelProperty");
        HEADER_ALIAS.put("channel_property", "channelProperty");
        HEADER_ALIAS.put("渠道定义", "channelDef");
        HEADER_ALIAS.put("业务类型", "channelDef");      // 兼容旧模板
        HEADER_ALIAS.put("channel_def", "channelDef");
        HEADER_ALIAS.put("profession_type", "channelDef"); // 兼容旧列名
        HEADER_ALIAS.put("店仓名称", "storeName");
        HEADER_ALIAS.put("店铺名称", "storeName");      // 兼容旧模板
        HEADER_ALIAS.put("store_name", "storeName");
        HEADER_ALIAS.put("店仓品牌", "brandName");
        HEADER_ALIAS.put("品牌名称", "brandName");      // 兼容旧模板
        HEADER_ALIAS.put("brand_name", "brandName");
        HEADER_ALIAS.put("预算月份", "monthlyName");
        HEADER_ALIAS.put("月份", "monthlyName");          // 兼容旧模板
        HEADER_ALIAS.put("monthly_name", "monthlyName");
        HEADER_ALIAS.put("预算日期", "budgetDtm");
        HEADER_ALIAS.put("budget_dtm", "budgetDtm");
        HEADER_ALIAS.put("预算金额", "budgetAmount");
        HEADER_ALIAS.put("budget_amount", "budgetAmount");
        HEADER_ALIAS.put("渠道性质", "businessType");
        HEADER_ALIAS.put("business_type", "businessType");
        HEADER_ALIAS.put("经营类型", "businessProperty");
        HEADER_ALIAS.put("business_property", "businessProperty");
        HEADER_ALIAS.put("销售类型", "salesType");
        HEADER_ALIAS.put("sales_type", "salesType");
    }

    @Autowired
    private SalesBudgetFillDailyMapper budgetMapper;

    @Autowired
    private BudgetImportLogMapper importLogMapper;

    /** bidw 数据源事务管理器（MERGE 入库用） */
    @Autowired
    @Qualifier("bidwTransactionManager")
    private PlatformTransactionManager bidwTransactionManager;

    @Override
    public PageResult<SalesBudgetFillDaily> page(PageQuery pageQuery, SalesBudgetQueryDTO queryDTO) {
        Page<SalesBudgetFillDaily> page = pageQuery.toPage();
        LambdaQueryWrapper<SalesBudgetFillDaily> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getStoreName())) {
                wrapper.like(SalesBudgetFillDaily::getStoreName, queryDTO.getStoreName());
            }
            if (StringUtils.hasText(queryDTO.getBrandName())) {
                wrapper.like(SalesBudgetFillDaily::getBrandName, queryDTO.getBrandName());
            }
            if (StringUtils.hasText(queryDTO.getChannelDef())) {
                wrapper.like(SalesBudgetFillDaily::getChannelDef, queryDTO.getChannelDef());
            }
            if (queryDTO.getBudgetDtmStart() != null) {
                wrapper.ge(SalesBudgetFillDaily::getBudgetDtm, queryDTO.getBudgetDtmStart());
            }
            if (queryDTO.getBudgetDtmEnd() != null) {
                wrapper.le(SalesBudgetFillDaily::getBudgetDtm, queryDTO.getBudgetDtmEnd());
            }
        }
        // 默认查询最近6个月的预算（当用户没有指定日期范围时）
        if (queryDTO == null || (queryDTO.getBudgetDtmStart() == null && queryDTO.getBudgetDtmEnd() == null)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -6);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date sixMonthsAgo = cal.getTime();
            wrapper.ge(SalesBudgetFillDaily::getBudgetDtm, sixMonthsAgo);
        }
        wrapper.orderByDesc(SalesBudgetFillDaily::getBudgetDtm);
        Page<SalesBudgetFillDaily> result = budgetMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // 0. 文件格式检测
        String realFormat = detectFormat(file);
        log.info("店铺日预算 导入文件真实格式: {}", realFormat);
        if (!"xlsx".equals(realFormat) && !"xls".equals(realFormat)) {
            return buildResult(0, 0, 0, Collections.singletonList(
                    "文件不是标准的 Excel 格式（检测为 " + realFormat + "），请用 Excel 打开后另存为 .xlsx 再上传"));
        }

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        Map<String, Integer> columnIndex = new HashMap<>();
        // 文件内去重：store_name + brand_name + budget_dtm 组合键
        Set<String> batchKeys = new HashSet<>();
        List<SalesBudgetFillDaily> buffer = new ArrayList<>(BATCH_SIZE);

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
                SalesBudgetFillDaily entity = mapRow(rowCells, columnIndex);
                // 校验必填字段（表定义中 store_name/brand_name/budget_dtm/budget_amount 为 NOT NULL）
                if (!StringUtils.hasText(entity.getStoreName())) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：店铺名称不能为空");
                    return;
                }
                if (!StringUtils.hasText(entity.getBrandName())) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：品牌名称不能为空");
                    return;
                }
                if (entity.getBudgetDtm() == null) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：预算日期不能为空");
                    return;
                }
                if (entity.getBudgetAmount() == null) {
                    if (errors.size() < MAX_ERRORS) errors.add("第" + rowNum + "行：预算金额不能为空");
                    return;
                }

                String key = entity.getStoreName() + "|" + entity.getBrandName() + "|" + entity.getBudgetDtm().getTime();
                if (batchKeys.contains(key)) {
                    buffer.removeIf(e -> key.equals(e.getStoreName() + "|" + e.getBrandName() + "|" + e.getBudgetDtm().getTime()));
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

        // 表头缺失校验（必填列）
        List<String> missingHeaders = new ArrayList<>();
        if (!columnIndex.containsKey("storeName")) missingHeaders.add("店铺名称");
        if (!columnIndex.containsKey("brandName")) missingHeaders.add("品牌名称");
        if (!columnIndex.containsKey("budgetDtm")) missingHeaders.add("预算日期");
        if (!columnIndex.containsKey("budgetAmount")) missingHeaders.add("预算金额");
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
        log.info("店铺日预算 导入完成: total={}, success={}, fail={}", total.get(), success.get(), fail);

        String status = (total.get() == 0) ? "FAILED" : (fail == 0 ? "SUCCESS" : "PARTIAL");
        saveImportLog(file, total.get(), success.get(), fail, status, errors);

        return buildResult(total.get(), success.get(), fail, errors);
    }

    @Override
    public PageResult<BudgetImportLog> logPage(PageQuery pageQuery) {
        Page<BudgetImportLog> page = importLogMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<BudgetImportLog>().orderByDesc(BudgetImportLog::getId));
        return PageResult.of(page);
    }

    private void flushBatch(List<SalesBudgetFillDaily> buffer, Set<String> batchKeys,
                            AtomicInteger success, List<String> errors) {
        if (buffer.isEmpty()) return;
        List<SalesBudgetFillDaily> toWrite = new ArrayList<>(buffer);
        buffer.clear();
        batchKeys.clear();
        // MERGE 走 bidw 数据源事务
        TransactionTemplate txTemplate = new TransactionTemplate(bidwTransactionManager);
        try {
            txTemplate.execute(status -> {
                budgetMapper.mergeBatch(toWrite);
                return null;
            });
            success.addAndGet(toWrite.size());
        } catch (Exception e) {
            log.error("店铺日预算 批量入库失败", e);
            if (errors.size() < MAX_ERRORS) errors.add("批量入库失败: " + e.getMessage());
        }
    }

    private SalesBudgetFillDaily mapRow(List<Object> cells, Map<String, Integer> columnIndex) {
        SalesBudgetFillDaily e = new SalesBudgetFillDaily();
        boolean useHeader = !columnIndex.isEmpty();
        e.setRegionLevel1(cellStr(cells, useHeader ? columnIndex.get("regionLevel1") : null));
        e.setRegionLevel2(cellStr(cells, useHeader ? columnIndex.get("regionLevel2") : null));
        e.setChannelProperty(cellStr(cells, useHeader ? columnIndex.get("channelProperty") : null));
        e.setChannelDef(cellStr(cells, useHeader ? columnIndex.get("channelDef") : null));
        e.setStoreName(cellStr(cells, useHeader ? columnIndex.get("storeName") : null));
        e.setBrandName(cellStr(cells, useHeader ? columnIndex.get("brandName") : null));
        e.setMonthlyName(cellStr(cells, useHeader ? columnIndex.get("monthlyName") : null));
        e.setBudgetDtm(parseDate(cellStr(cells, useHeader ? columnIndex.get("budgetDtm") : null)));
        e.setBudgetAmount(parseBigDecimal(cellStr(cells, useHeader ? columnIndex.get("budgetAmount") : null)));
        e.setBusinessType(cellStr(cells, useHeader ? columnIndex.get("businessType") : null));
        e.setBusinessProperty(cellStr(cells, useHeader ? columnIndex.get("businessProperty") : null));
        e.setSalesType(cellStr(cells, useHeader ? columnIndex.get("salesType") : null));
        return e;
    }

    private String cellStr(List<Object> cells, Integer idx) {
        if (idx == null || idx < 0 || idx >= cells.size()) return null;
        Object v = cells.get(idx);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private java.util.Date parseDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        // 兼容多种日期格式
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss"};
        for (String p : patterns) {
            try {
                return new java.text.SimpleDateFormat(p).parse(s);
            } catch (java.text.ParseException ignored) {
            }
        }
        // Excel 数字日期（自 1899-12-30 起的天数）
        try {
            double d = Double.parseDouble(s);
            if (d > 0 && d < 100000) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(1899, java.util.Calendar.DECEMBER, 30, 0, 0, 0);
                c.add(java.util.Calendar.DATE, (int) d);
                return c.getTime();
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
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
                log.info("店铺日预算 xlsx 共 {} 个 sheet", sheetCount);
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
            BudgetImportLog logEntity = new BudgetImportLog();
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
