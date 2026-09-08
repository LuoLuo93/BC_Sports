package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.bi.DwSalesImportLog;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import com.bcsport.admin.service.OdsSalesMainService;
import com.bcsport.admin.mapper.DwSalesImportLogMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数仓销售查看（BI_DW.ODS_SALES_MAIN 查询 + 编辑 + Excel批量导入）
 */
@Slf4j
@RestController
@RequestMapping("/api/bi/dw-sales")
@Api(tags = "数仓销售查看")
public class OdsSalesMainController {

    private static final long MAX_IMPORT_SIZE = 500 * 1024 * 1024L;

    @Autowired
    private OdsSalesMainService odsSalesMainService;

    @Autowired
    private DwSalesImportLogMapper importLogMapper;

    /**
     * 分页查询销售主明细
     */
    @GetMapping("/page")
    @ApiOperation("分页查询销售主明细")
    @RequiresPermissions("bi:dw-sales:query")
    public Result<PageResult<OdsSalesMain>> page(PageQuery pageQuery, OdsSalesMainQueryDTO queryDTO) {
        return Result.success(odsSalesMainService.page(pageQuery, queryDTO));
    }

    /**
     * 编辑归属维度字段
     */
    @PostMapping("/update")
    @ApiOperation("编辑销售明细归属维度字段")
    @RequiresPermissions("bi:dw-sales:edit")
    // 事实表人工改数必须留痕：@OperLog 把 OdsSalesMainUpdateDTO(billId/itemId/改动字段)序列化进 sys_log，
    // 与导入侧的 DwSalesImportLog 形成对称审计
    @OperLog(module = "数仓销售", operation = "编辑销售明细")
    public Result<?> update(@Valid @RequestBody OdsSalesMainUpdateDTO dto) {
        boolean success = odsSalesMainService.update(dto);
        return success ? Result.success("修改成功") : Result.error("记录不存在(可能已被ETL重灌)，请刷新后重试");
    }

    /**
     * 上传 Excel 期初数据导入（150W行级：SAX流式 + 分批INSERT，同步返回，前端超时30分钟；不防重）
     */
    @PostMapping("/import")
    @ApiOperation("上传Excel期初数据导入")
    @RequiresPermissions("bi:dw-sales:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        if (file.getSize() > MAX_IMPORT_SIZE) {
            return Result.paramError("文件大小不能超过500MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        try {
            Map<String, Object> result = odsSalesMainService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("数仓销售导入 Excel解析失败: {}", e.getMessage());
            String errorMsg = "Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式: " + e.getMessage();
            saveFailedLog(file, 0, 0, 0, errorMsg);
            return Result.error(errorMsg);
        } catch (Exception e) {
            log.error("数仓销售导入失败: {}", e.getMessage(), e);
            String errorMsg = "导入失败：" + e.getMessage();
            saveFailedLog(file, 0, 0, 0, errorMsg);
            return Result.error(errorMsg);
        }
    }

    /**
     * 导入日志分页查询
     */
    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("bi:dw-sales:query")
    public Result<PageResult<DwSalesImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(odsSalesMainService.logPage(pageQuery));
    }

    /**
     * 下载导入模板（表头 + 样例行）
     */
    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("bi:dw-sales:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("数仓销售导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            // 模板列结构与真实导入文件一致(英文表头)；BILL_ID/ITEM_ID 留空=导入时自动生成
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("BILL_ID", null);
            sample.put("BILL_NO", "OS230626BCP0202-002");
            sample.put("BILL_DATE", "20230626");
            sample.put("BILL_TIME", "2023-06-26 17:56:23");
            sample.put("VIP_CODE", null);
            sample.put("VIP_MOBILE", null);
            sample.put("OMS_SOURCECODE", null);
            sample.put("SALES_TYPE", "零售");
            sample.put("ITEM_ID", null);
            sample.put("BILL_POS_ID", null);
            sample.put("BILL_POS_CODE", null);
            sample.put("BILL_POS_NAME", "张灵灵");
            sample.put("STORE_ID", null);
            sample.put("STORE_CODE", "5749901");
            sample.put("STORE_NAME", "宁波银泰东门NL");
            sample.put("PRODUCT_CODE", "NKJCT2308S3048NL");
            sample.put("PRODUCT_STYLE_NO", "NKJCT2308S");
            sample.put("PRODUCT_NAME", "布拉迪女式外套");
            sample.put("COLORSALIAS", "薄粉色");
            sample.put("BARCODE", "NKJCT2308S3048NL170");
            sample.put("SIZES", "170");
            sample.put("QTY", "1");
            sample.put("RETAIL_PRICE", "798");
            sample.put("RETAIL_AMOUNT", "798");
            sample.put("TRANSACTION_AMOUNT", "319.2");
            sample.put("REVENUE", "319.2");
            sample.put("RECALC_REVENUE", "319.2");
            sample.put("NEW_OLD_NAME_ADJUST", null);
            sample.put("ANCHOR_SUMMARYID", null);
            sample.put("ANCHOR_SUMMARYNAME", null);
            sample.put("PROMOTION_NAME", "沪杭618 C店 折扣单");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    private void saveFailedLog(MultipartFile file, int total, int success, int fail, String errorMsg) {
        try {
            DwSalesImportLog logEntity = new DwSalesImportLog();
            logEntity.setFileName(file.getOriginalFilename());
            logEntity.setFileSize(file.getSize());
            logEntity.setTotalCount(total);
            logEntity.setSuccessCount(success);
            logEntity.setFailCount(fail);
            logEntity.setStatus("FAILED");
            logEntity.setErrorMsg(errorMsg.length() > 4000 ? errorMsg.substring(0, 4000) : errorMsg);
            logEntity.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
            logEntity.setCreateTime(LocalDateTime.now());
            importLogMapper.insert(logEntity);
        } catch (Exception ex) {
            log.warn("保存数仓销售导入失败日志失败: {}", ex.getMessage());
        }
    }
}
