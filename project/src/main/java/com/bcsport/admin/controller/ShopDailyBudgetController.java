package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.SalesBudgetQueryDTO;
import com.bcsport.admin.entity.bi.BudgetImportLog;
import com.bcsport.admin.entity.bi.SalesBudgetFillDaily;
import com.bcsport.admin.service.SalesBudgetFillDailyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 店铺日预算（查询 + Excel 导入 + 导入日志）
 */
@Slf4j
@RestController
@RequestMapping("/api/bi/shop-daily-budget")
@Api(tags = "店铺日预算")
public class ShopDailyBudgetController {

    @Autowired
    private SalesBudgetFillDailyService salesBudgetService;

    /**
     * 分页查询店铺日预算
     */
    @GetMapping("/page")
    @ApiOperation("分页查询店铺日预算")
    @RequiresPermissions("bi:shop-daily-budget:query")
    public Result<PageResult<SalesBudgetFillDaily>> page(PageQuery pageQuery, SalesBudgetQueryDTO queryDTO) {
        return Result.success(salesBudgetService.page(pageQuery, queryDTO));
    }

    /**
     * 上传 Excel 批量导入
     */
    @PostMapping("/import")
    @ApiOperation("上传Excel批量导入")
    @RequiresPermissions("bi:shop-daily-budget:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        if (file.getSize() > 100 * 1024 * 1024) {
            return Result.paramError("文件大小不能超过100MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        try {
            Map<String, Object> result = salesBudgetService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("店铺日预算 Excel解析失败: {}", e.getMessage());
            return Result.error("Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式");
        } catch (Exception e) {
            log.error("店铺日预算 导入失败: {}", e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 导入日志分页查询
     */
    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("bi:shop-daily-budget:query")
    public Result<PageResult<BudgetImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(salesBudgetService.logPage(pageQuery));
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("bi:shop-daily-budget:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("店铺日预算导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            // 表头别名（中文）
            writer.addHeaderAlias("department1", "一级组织");
            writer.addHeaderAlias("department2", "二级组织");
            writer.addHeaderAlias("channelProperty", "渠道类型");
            writer.addHeaderAlias("professionType", "业务类型");
            writer.addHeaderAlias("storeName", "店铺名称");
            writer.addHeaderAlias("brandName", "品牌名称");
            writer.addHeaderAlias("monthlyName", "月份");
            writer.addHeaderAlias("budgetDtm", "预算日期");
            writer.addHeaderAlias("budgetAmount", "预算金额");
            writer.addHeaderAlias("businessType", "渠道性质");
            writer.addHeaderAlias("businessProperty", "经营类型");
            writer.addHeaderAlias("salesType", "销售类型");

            // 示例数据
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("department1", "华东大区");
            sample.put("department2", "上海分公司");
            sample.put("channelProperty", "直营");
            sample.put("professionType", "零售");
            sample.put("storeName", "上海旗舰店");
            sample.put("brandName", "NORTHLAND");
            sample.put("monthlyName", "2026-08");
            sample.put("budgetDtm", "2026-08-01");
            sample.put("budgetAmount", "50000.00");
            sample.put("businessType", "线上");
            sample.put("businessProperty", "自营");
            sample.put("salesType", "内销");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }
}
