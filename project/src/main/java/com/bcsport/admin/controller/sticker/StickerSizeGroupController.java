package com.bcsport.admin.controller.sticker;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.sticker.StickerSize;
import com.bcsport.admin.entity.sticker.StickerSizeGroup;
import com.bcsport.admin.service.sticker.StickerSizeGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 贴纸本地尺码组维护
 */
@RestController
@RequestMapping("/api/sticker/size-group")
@Api(tags = "本地尺码组维护")
@Slf4j
public class StickerSizeGroupController {

    @Autowired
    private StickerSizeGroupService service;

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("sticker:size-group:query")
    public Result<PageResult<StickerSizeGroup>> page(@Valid PageQuery pageQuery,
                                                     @RequestParam(required = false) String brandId,
                                                     @RequestParam(required = false) String kindId,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) String groupCode,
                                                     @RequestParam(required = false) String groupName) {
        return Result.success(service.page(pageQuery, brandId, kindId, status, groupCode, groupName));
    }

    @GetMapping("/list")
    @ApiOperation("按品牌+类别查启用组列表(供明细行下拉)")
    @RequiresPermissions("sticker:size-group:query")
    public Result<List<StickerSizeGroup>> list(@RequestParam(required = false) String brandId,
                                               @RequestParam(required = false) String kindId) {
        return Result.success(service.listActiveByBrandKind(brandId, kindId));
    }

    @GetMapping("/{id}/sizes")
    @ApiOperation("查某组下尺码明细")
    @RequiresPermissions("sticker:size-group:query")
    public Result<List<StickerSize>> sizes(@PathVariable String id) {
        return Result.success(service.listSizesByGroupId(id));
    }

    @GetMapping("/{id}")
    @ApiOperation("详情(含尺码明细)")
    @RequiresPermissions("sticker:size-group:query")
    public Result<StickerSizeGroup> getById(@PathVariable String id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    @ApiOperation("新增")
    @RequiresPermissions("sticker:size-group:add")
    public Result<?> create(@RequestBody StickerSizeGroup entity) {
        service.create(entity);
        return Result.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @ApiOperation("修改")
    @RequiresPermissions("sticker:size-group:edit")
    public Result<?> update(@PathVariable String id, @RequestBody StickerSizeGroup entity) {
        service.update(id, entity);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除")
    @RequiresPermissions("sticker:size-group:delete")
    public Result<?> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/import")
    @ApiOperation("Excel 批量导入尺码组")
    @RequiresPermissions("sticker:size-group:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.paramError("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            return Result.paramError("仅支持 .xlsx / .xls 格式");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            return Result.paramError("文件大小不能超过 50MB");
        }
        try {
            Map<String, Object> result = service.importFromExcel(file);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("sticker:size-group:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode("尺码组导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            // 固定列
            writer.addHeaderAlias("groupCode", "组编码");
            writer.addHeaderAlias("groupName", "组名称");
            writer.addHeaderAlias("brandName", "品牌");
            writer.addHeaderAlias("kindName", "类别");
            writer.addHeaderAlias("sort", "排序");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("remark", "备注");

            // 示例行1：单行完整尺码
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("groupCode", "AD-OUTERWEAR");
            row1.put("groupName", "阿迪外衣尺码组");
            row1.put("brandName", "ADIDAS");
            row1.put("kindName", "外衣");
            row1.put("sort", "1");
            row1.put("status", "1");
            row1.put("remark", "");
            // 尺码列(动态列，每两列一对：编码+名称)
            row1.put("sizeCode1", "S");
            row1.put("sizeName1", "160");
            row1.put("sizeCode2", "M");
            row1.put("sizeName2", "165");
            row1.put("sizeCode3", "L");
            row1.put("sizeName3", "170");
            row1.put("sizeCode4", "XL");
            row1.put("sizeName4", "175");

            // 示例行2：同组编码+品牌+类别，追加尺码（演示多行合并）
            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("groupCode", "AD-OUTERWEAR");
            row2.put("groupName", "阿迪外衣尺码组");
            row2.put("brandName", "ADIDAS");
            row2.put("kindName", "外衣");
            row2.put("sort", "");
            row2.put("status", "");
            row2.put("remark", "");
            row2.put("sizeCode1", "XXL");
            row2.put("sizeName1", "180");
            row2.put("sizeCode2", "XXXL");
            row2.put("sizeName2", "185");

            // 示例行3：不同组
            Map<String, Object> row3 = new LinkedHashMap<>();
            row3.put("groupCode", "NK-TSHIRT");
            row3.put("groupName", "耐克T恤尺码组");
            row3.put("brandName", "NIKE");
            row3.put("kindName", "T恤");
            row3.put("sort", "2");
            row3.put("status", "1");
            row3.put("remark", "");
            row3.put("sizeCode1", "XS");
            row3.put("sizeName1", "155");
            row3.put("sizeCode2", "S");
            row3.put("sizeName2", "160");
            row3.put("sizeCode3", "M");
            row3.put("sizeName3", "165");
            row3.put("sizeCode4", "L");
            row3.put("sizeName4", "170");

            writer.addHeaderAlias("sizeCode1", "尺码1编码");
            writer.addHeaderAlias("sizeName1", "尺码1名称");
            writer.addHeaderAlias("sizeCode2", "尺码2编码");
            writer.addHeaderAlias("sizeName2", "尺码2名称");
            writer.addHeaderAlias("sizeCode3", "尺码3编码");
            writer.addHeaderAlias("sizeName3", "尺码3名称");
            writer.addHeaderAlias("sizeCode4", "尺码4编码");
            writer.addHeaderAlias("sizeName4", "尺码4名称");

            writer.write(Arrays.asList(row1, row2, row3), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }
}
