package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.bi.GoodsOldNew;
import com.bcsport.admin.entity.bi.GoodsImportLog;
import com.bcsport.admin.service.GoodsOldNewService;
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
 * 货品资料导入
 */
@Slf4j
@RestController
@RequestMapping("/api/bi/goods-data")
@Api(tags = "货品资料")
public class GoodsOldNewController {

    @Autowired
    private GoodsOldNewService goodsOldNewService;

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("bi:goods-data:query")
    public Result<PageResult<GoodsOldNew>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String brand,
                                                @RequestParam(required = false) String articleNo) {
        return Result.success(goodsOldNewService.page(pageQuery, brand, articleNo));
    }

    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("bi:goods-data:query")
    public Result<PageResult<GoodsImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(goodsOldNewService.logPage(pageQuery));
    }

    @PostMapping("/import")
    @ApiOperation("上传Excel批量导入")
    @RequiresPermissions("bi:goods-data:import")
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
            Map<String, Object> result = goodsOldNewService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("GoodsOldNew Excel解析失败: {}", e.getMessage());
            return Result.error("Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式");
        } catch (Exception e) {
            log.error("GoodsOldNew 导入失败: {}", e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("bi:goods-data:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("货品新旧资料导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("品牌", "品牌");
            writer.addHeaderAlias("货号", "货号");
            writer.addHeaderAlias("产品季", "产品季");
            writer.addHeaderAlias("货品分类", "货品分类");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("品牌", "NORTHLAND");
            sample.put("货号", "AA12345");
            sample.put("产品季", "26SS");
            sample.put("货品分类", "当季新品");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }
}
