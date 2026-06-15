package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.service.sticker.BrandTemplateMatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sticker/brand-template")
@Api(tags = "品牌模板关系管理")
public class BrandTemplateMatchController {

    @Autowired
    private BrandTemplateMatchService service;

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<PageResult<BrandTemplateMatch>> page(@Valid PageQuery pageQuery,
                                                       @RequestParam(required = false) String brandName,
                                                       @RequestParam(required = false) String kindName) {
        return Result.success(service.page(pageQuery, brandName, kindName));
    }

    @GetMapping("/list")
    @ApiOperation("全部列表（供下拉选择）")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<List<BrandTemplateMatch>> list() {
        return Result.success(service.listAll());
    }

    @GetMapping("/kinds")
    @ApiOperation("类别列表（从ERP查询）")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<?> getKinds() {
        return Result.success(bjerpProductMapper.getKinds());
    }

    @GetMapping("/{id}")
    @ApiOperation("详情")
    @RequiresPermissions("sticker:brand-template:query")
    public Result<BrandTemplateMatch> getById(@PathVariable String id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    @ApiOperation("新增")
    @RequiresPermissions("sticker:brand-template:add")
    public Result<?> create(@Valid @RequestBody BrandTemplateMatch entity) {
        service.create(entity);
        return Result.success("新增成功");
    }

    @PutMapping("/{id}")
    @ApiOperation("修改")
    @RequiresPermissions("sticker:brand-template:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody BrandTemplateMatch entity) {
        service.update(id, entity);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除")
    @RequiresPermissions("sticker:brand-template:delete")
    public Result<?> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success("删除成功");
    }
}
