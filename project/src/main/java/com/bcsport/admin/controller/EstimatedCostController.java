package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.bi.EstimatedCostImportLog;
import com.bcsport.admin.mapper.EstimatedCostImportLogMapper;
import com.bcsport.admin.service.EstimatedCostService;
import com.bcsport.admin.util.ShiroSecurityUtils;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * 预估成本管理（伯俊 ERP M_PRODUCT.PRECOST）
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/estimated-cost")
@Api(tags = "预估成本管理")
public class EstimatedCostController {

    @Autowired
    private EstimatedCostService estimatedCostService;

    @Autowired
    private EstimatedCostImportLogMapper importLogMapper;

    @GetMapping("/page")
    @ApiOperation("分页查询预估成本")
    @RequiresPermissions("erp:estimatedCost:query")
    public Result<PageResult<Map<String, Object>>> page(PageQuery pageQuery,
                                                         @RequestParam(required = false) String materialNumber,
                                                         @RequestParam(required = false) String styleNumber,
                                                         @RequestParam(required = false) String materialName) {
        // 数据量较大，必须至少传入一个查询条件才查询，无条件直接返回空（前端默认不查询，此处兜底）
        if (isAllEmpty(materialNumber, styleNumber, materialName)) {
            return Result.success(PageResult.empty(pageQuery));
        }
        return Result.success(estimatedCostService.page(pageQuery, materialNumber, styleNumber, materialName));
    }

    @PutMapping("/precost")
    @ApiOperation("编辑预估成本")
    @RequiresPermissions("erp:estimatedCost:edit")
    public Result<?> updatePrecost(@RequestBody Map<String, Object> body) {
        String materialNumber = body.get("materialNumber") == null ? null : body.get("materialNumber").toString();
        String precost = body.get("precost") == null ? null : body.get("precost").toString();
        estimatedCostService.updatePrecost(materialNumber, precost);
        return Result.success("保存成功");
    }

    @PostMapping("/import")
    @ApiOperation("上传Excel批量导入预估成本")
    @RequiresPermissions("erp:estimatedCost:import")
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
            Map<String, Object> result = estimatedCostService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("预估成本 Excel解析失败: {}", e.getMessage());
            String errorMsg = "Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式: " + e.getMessage();
            saveFailedLog(file, errorMsg);
            return Result.error(errorMsg);
        } catch (Exception e) {
            log.error("预估成本导入失败: {}", e.getMessage(), e);
            String errorMsg = "导入失败：" + e.getMessage();
            saveFailedLog(file, errorMsg);
            return Result.error(errorMsg);
        }
    }

    private void saveFailedLog(MultipartFile file, String errorMsg) {
        try {
            EstimatedCostImportLog logEntity = new EstimatedCostImportLog();
            logEntity.setFileName(file.getOriginalFilename());
            logEntity.setFileSize(file.getSize());
            logEntity.setTotalCount(0);
            logEntity.setSuccessCount(0);
            logEntity.setFailCount(0);
            logEntity.setStatus("FAILED");
            logEntity.setErrorMsg(errorMsg.length() > 4000 ? errorMsg.substring(0, 4000) : errorMsg);
            logEntity.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
            logEntity.setCreateTime(LocalDateTime.now());
            importLogMapper.insert(logEntity);
        } catch (Exception ex) {
            log.warn("保存失败导入日志失败: {}", ex.getMessage());
        }
    }

    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("erp:estimatedCost:query")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("预估成本导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("货号", "货号");
            writer.addHeaderAlias("预估成本", "预估成本");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("货号", "NLM25001");
            sample.put("预估成本", "120.50");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("erp:estimatedCost:query")
    public Result<PageResult<EstimatedCostImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(estimatedCostService.logPage(pageQuery));
    }

    private boolean isAllEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return false;
        }
        return true;
    }
}
