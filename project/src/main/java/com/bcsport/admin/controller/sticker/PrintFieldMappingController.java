package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.FieldLabelUtils;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.sticker.PrintFieldMapping;
import com.bcsport.admin.entity.sticker.StickerPrintOrderDetail;
import com.bcsport.admin.service.sticker.PrintFieldMappingService;
import com.bcsport.admin.vo.FieldOption;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sticker/field-mapping")
@Api(tags = "打印字段映射")
public class PrintFieldMappingController {

    @Autowired
    private PrintFieldMappingService service;

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("sticker:field-mapping:query")
    public Result<PageResult<PrintFieldMapping>> page(@Valid PageQuery pageQuery,
                                                      @RequestParam(required = false) String templateId) {
        return Result.success(PageResult.of(service.page(pageQuery.getPageNum(), pageQuery.getPageSize(), templateId)));
    }

    @GetMapping("/list")
    @ApiOperation("按模板名称查询字段映射列表")
    @RequiresPermissions("sticker:field-mapping:query")
    public Result<List<PrintFieldMapping>> list(@RequestParam String templateName) {
        return Result.success(service.getByTemplateName(templateName));
    }

    @PostMapping
    @ApiOperation("新增")
    @RequiresPermissions("sticker:field-mapping:add")
    public Result<?> create(@RequestBody PrintFieldMapping entity) {
        service.create(entity);
        return Result.success("新增成功");
    }

    @PutMapping("/{id}")
    @ApiOperation("修改")
    @RequiresPermissions("sticker:field-mapping:edit")
    public Result<?> update(@PathVariable String id, @RequestBody PrintFieldMapping entity) {
        service.update(id, entity);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除")
    @RequiresPermissions("sticker:field-mapping:delete")
    public Result<?> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/by-template/{templateName}")
    @ApiOperation("按模板名称删除所有映射")
    @RequiresPermissions("sticker:field-mapping:delete")
    public Result<?> deleteByTemplateName(@PathVariable String templateName) {
        service.deleteByTemplateName(templateName);
        return Result.success("删除成功");
    }

    @GetMapping("/available-fields")
    @ApiOperation("获取可用的打印数据字段")
    @RequiresPermissions("sticker:field-mapping:query")
    public Result<List<FieldOption>> getAvailableFields() {
        return Result.success(FieldLabelUtils.getFields(StickerPrintOrderDetail.class));
    }
}
