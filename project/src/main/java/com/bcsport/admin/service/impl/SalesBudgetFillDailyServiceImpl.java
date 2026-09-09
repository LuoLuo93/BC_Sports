package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.SalesBudgetFillDailyMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SalesBudgetQueryDTO;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.bi.SalesBudgetFillDaily;
import com.bcsport.admin.importer.BatchCtx;
import com.bcsport.admin.importer.ExcelImportRunner;
import com.bcsport.admin.importer.ExcelImportSpec;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.importer.RowCtx;
import com.bcsport.admin.service.ImportLogService;
import com.bcsport.admin.service.SalesBudgetFillDailyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店铺日预算实现
 * 流程骨架由 ExcelImportRunner 承担（R01），本类只声明表头别名、行映射与 MERGE 入库策略（bidw 数据源）
 * extends ServiceImpl<SalesBudgetFillDailyMapper, SalesBudgetFillDaily>
 *   → baseMapper 自动绑定 SalesBudgetFillDailyMapper(走 bidw 数据源)
 */
@Service
public class SalesBudgetFillDailyServiceImpl
        extends ServiceImpl<SalesBudgetFillDailyMapper, SalesBudgetFillDaily>
        implements SalesBudgetFillDailyService {

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

    /** bidw 数据源事务管理器（MERGE 入库用） */
    @Autowired
    @Qualifier("bidwTransactionManager")
    private PlatformTransactionManager bidwTransactionManager;

    @Autowired
    private ExcelImportRunner importRunner;

    @Autowired
    private ImportLogService importLogService;

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
        wrapper.orderByAsc(SalesBudgetFillDaily::getStoreName)
               .orderByAsc(SalesBudgetFillDaily::getBrandName)
               .orderByDesc(SalesBudgetFillDaily::getBudgetDtm);
        Page<SalesBudgetFillDaily> result = budgetMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        return importRunner.run(file, new ExcelImportSpec<SalesBudgetFillDaily>() {
            @Override
            public String logLabel() {
                return "店铺日预算";
            }

            @Override
            public ImportType type() {
                return ImportType.SALES_BUDGET;
            }

            @Override
            public Map<String, String> headerAlias() {
                return HEADER_ALIAS;
            }

            @Override
            public void validateHeaders(Map<String, Integer> columnIndex) {
                List<String> missing = new ArrayList<>();
                if (!columnIndex.containsKey("storeName")) missing.add("店仓名称");
                if (!columnIndex.containsKey("brandName")) missing.add("店仓品牌");
                if (!columnIndex.containsKey("budgetDtm")) missing.add("预算日期");
                if (!columnIndex.containsKey("budgetAmount")) missing.add("预算金额");
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("Excel缺少必需列：" + String.join("、", missing) + "，请检查表头");
                }
            }

            /** 本模块无固定列序兜底：表头缺列即取不到值（列索引 null） */
            @Override
            public SalesBudgetFillDaily mapRow(RowCtx ctx) {
                SalesBudgetFillDaily e = new SalesBudgetFillDaily();
                e.setRegionLevel1(ctx.str("regionLevel1"));
                e.setRegionLevel2(ctx.str("regionLevel2"));
                e.setChannelProperty(ctx.str("channelProperty"));
                e.setChannelDef(ctx.str("channelDef"));
                e.setStoreName(ctx.str("storeName"));
                e.setBrandName(ctx.str("brandName"));
                e.setMonthlyName(ctx.str("monthlyName"));
                e.setBudgetDtm(parseDate(ctx.str("budgetDtm")));
                e.setBudgetAmount(parseBigDecimal(ctx.str("budgetAmount")));
                e.setBusinessType(ctx.str("businessType"));
                e.setBusinessProperty(ctx.str("businessProperty"));
                e.setSalesType(ctx.str("salesType"));
                // 表有9个NOT NULL列，全部必填，按原校验顺序
                if (!StringUtils.hasText(e.getRegionLevel1())) throw new IllegalArgumentException("一级地区不能为空");
                if (!StringUtils.hasText(e.getRegionLevel2())) throw new IllegalArgumentException("二级地区不能为空");
                if (!StringUtils.hasText(e.getChannelProperty())) throw new IllegalArgumentException("渠道类型不能为空");
                if (!StringUtils.hasText(e.getChannelDef())) throw new IllegalArgumentException("渠道定义不能为空");
                if (!StringUtils.hasText(e.getStoreName())) throw new IllegalArgumentException("店仓名称不能为空");
                if (!StringUtils.hasText(e.getBrandName())) throw new IllegalArgumentException("店仓品牌不能为空");
                if (!StringUtils.hasText(e.getMonthlyName())) throw new IllegalArgumentException("预算月份不能为空");
                if (e.getBudgetDtm() == null) throw new IllegalArgumentException("预算日期不能为空");
                if (e.getBudgetAmount() == null) throw new IllegalArgumentException("预算金额不能为空");
                return e;
            }

            /** 文件内去重：store_name + brand_name + budget_dtm 组合键 */
            @Override
            public String dedupKey(SalesBudgetFillDaily entity) {
                return entity.getStoreName() + "|" + entity.getBrandName() + "|" + entity.getBudgetDtm().getTime();
            }

            /** MERGE 走 bidw 数据源事务；批失败保留逐条明细错误（模块特有报文） */
            @Override
            public void onBatch(List<SalesBudgetFillDaily> batch, BatchCtx ctx) {
                List<SalesBudgetFillDaily> toWrite = new ArrayList<>(batch);
                try {
                    new TransactionTemplate(bidwTransactionManager).execute(status -> {
                        budgetMapper.mergeBatch(toWrite);
                        return null;
                    });
                    ctx.success(toWrite.size());
                } catch (Exception e) {
                    String reason = e.getMessage() == null ? "未知错误" : e.getMessage();
                    for (SalesBudgetFillDaily item : toWrite) {
                        ctx.error("入库失败 [店仓=" + item.getStoreName()
                                + ", 品牌=" + item.getBrandName()
                                + ", 日期=" + item.getBudgetDtm()
                                + "]: " + reason);
                    }
                }
            }

            /** 兜底：有失败但无错误明细时补一条说明 */
            @Override
            public void onFinish(int total, int success, int fail, List<String> errors) {
                if (fail > 0 && errors.isEmpty()) {
                    errors.add("共 " + fail + " 条数据未导入（可能因必填字段为空、数据类型不匹配或数据库约束冲突），请检查源数据");
                }
            }
        }).toResultMap();
    }

    @Override
    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.SALES_BUDGET, pageQuery);
    }

    private static java.util.Date parseDate(String s) {
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

    private static BigDecimal parseBigDecimal(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return new BigDecimal(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
