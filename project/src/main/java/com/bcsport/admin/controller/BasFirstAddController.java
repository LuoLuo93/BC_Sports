package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.qywx.BasFirstAdd;
import com.bcsport.admin.entity.qywx.BasFirstAddImportLog;
import com.bcsport.admin.service.BasFirstAddService;
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
 * 客户首次添加记录导入
 */
@Slf4j
@RestController
@RequestMapping("/api/bi/first-add")
@Api(tags = "客户首次添加记录")
public class BasFirstAddController {

    @Autowired
    private BasFirstAddService basFirstAddService;

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("bi:first-add:import")
    public Result<PageResult<BasFirstAdd>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String customerId,
                                                @RequestParam(required = false) String firstAdder) {
        return Result.success(basFirstAddService.page(pageQuery, customerId, firstAdder));
    }

    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("bi:first-add:import")
    public Result<PageResult<BasFirstAddImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(basFirstAddService.logPage(pageQuery));
    }

    @PostMapping("/import")
    @ApiOperation("上传Excel批量导入")
    @RequiresPermissions("bi:first-add:import")
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
            Map<String, Object> result = basFirstAddService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("Bas_FirstAdd Excel解析失败: {}", e.getMessage());
            return Result.error("Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式（建议用 Excel 打开后另存为 .xlsx 再上传）");
        } catch (Exception e) {
            log.error("Bas_FirstAdd 导入失败: {}", e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("bi:first-add:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("客户首次添加导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("客户id", "客户id");
            writer.addHeaderAlias("首次添加时间", "首次添加时间");
            writer.addHeaderAlias("首次添加人部门", "首次添加人部门");
            writer.addHeaderAlias("首次添加人店铺", "首次添加人店铺");
            writer.addHeaderAlias("首次添加人", "首次添加人");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("客户id", "wmABC123...");
            sample.put("首次添加时间", "2026-06-01 10:00:00");
            sample.put("首次添加人部门", "华东大区");
            sample.put("首次添加人店铺", "上海徐汇店");
            sample.put("首次添加人", "张三");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }
}
