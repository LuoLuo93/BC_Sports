package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.sticker.StickerSize;
import com.bcsport.admin.entity.sticker.StickerSizeGroup;
import com.bcsport.admin.service.sticker.StickerSizeGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

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
}
