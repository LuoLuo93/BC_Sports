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
 * 贴纸矫正尺码组维护
 */
@RestController
@RequestMapping("/api/sticker/size-group")
@Api(tags = "矫正尺码组维护")
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
            // 纵向布局：每行一个尺码，组信息在每行重复，同组编码+品牌+类别的多行自动合并
            writer.addHeaderAlias("groupCode", "组编码");
            writer.addHeaderAlias("groupName", "组名称");
            writer.addHeaderAlias("brandName", "品牌");
            writer.addHeaderAlias("kindName", "类别");
            writer.addHeaderAlias("sizeCode", "尺码编码");
            writer.addHeaderAlias("sizeName", "尺码名称");
            writer.addHeaderAlias("sort", "排序");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("remark", "备注");

            // 示例数据：每个尺码占一行，同一组的组信息(编码/名称/品牌/类别/排序/状态/备注)逐行重复
            List<Map<String, Object>> rows = new ArrayList<>();
            // 阿迪外衣尺码组：4 个尺码
            rows.add(buildVerticalRow("AD-OUTERWEAR", "阿迪外衣尺码组", "ADIDAS", "外衣", "S", "160", "1", "1", ""));
            rows.add(buildVerticalRow("AD-OUTERWEAR", "阿迪外衣尺码组", "ADIDAS", "外衣", "M", "165", "", "", ""));
            rows.add(buildVerticalRow("AD-OUTERWEAR", "阿迪外衣尺码组", "ADIDAS", "外衣", "L", "170", "", "", ""));
            rows.add(buildVerticalRow("AD-OUTERWEAR", "阿迪外衣尺码组", "ADIDAS", "外衣", "XL", "175", "", "", ""));
            // 耐克T恤尺码组：3 个尺码
            rows.add(buildVerticalRow("NK-TSHIRT", "耐克T恤尺码组", "NIKE", "T恤", "XS", "155", "2", "1", ""));
            rows.add(buildVerticalRow("NK-TSHIRT", "耐克T恤尺码组", "NIKE", "T恤", "S", "160", "", "", ""));
            rows.add(buildVerticalRow("NK-TSHIRT", "耐克T恤尺码组", "NIKE", "T恤", "M", "165", "", "", ""));

            writer.write(rows, true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    /** 构建纵向布局(每行一个尺码)的数据行 */
    private static Map<String, Object> buildVerticalRow(String groupCode, String groupName, String brandName,
                                                        String kindName, String sizeCode, String sizeName,
                                                        String sort, String status, String remark) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("groupCode", groupCode);
        row.put("groupName", groupName);
        row.put("brandName", brandName);
        row.put("kindName", kindName);
        row.put("sizeCode", sizeCode);
        row.put("sizeName", sizeName);
        row.put("sort", sort);
        row.put("status", status);
        row.put("remark", remark);
        return row;
    }
}
