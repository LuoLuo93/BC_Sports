package com.bcsport.admin.controller.sticker;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.sticker.StickerTemplate;
import com.bcsport.admin.service.sticker.StickerTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sticker/template")
@Api(tags = "标签模板管理")
public class StickerTemplateController {

    @Autowired
    private StickerTemplateService templateService;

    @GetMapping("/list")
    @ApiOperation("模板列表（不分页，供下拉选择等场景使用）")
    @RequiresPermissions("sticker:template:query")
    public Result<List<StickerTemplate>> list(@RequestParam(required = false) String templateName) {
        return Result.success(templateService.listTemplates(templateName));
    }

    @GetMapping("/page")
    @ApiOperation("模板分页列表")
    @RequiresPermissions("sticker:template:query")
    public Result<PageResult<StickerTemplate>> page(@Valid PageQuery pageQuery,
                                                     @RequestParam(required = false) String templateName) {
        return Result.success(templateService.pageTemplates(pageQuery, templateName));
    }

    @GetMapping("/{id}")
    @ApiOperation("模板详情")
    @RequiresPermissions("sticker:template:query")
    public Result<StickerTemplate> getById(@PathVariable String id) {
        return Result.success(templateService.getTemplate(id));
    }

    @PostMapping
    @ApiOperation("创建模板")
    @RequiresPermissions("sticker:template:add")
    public Result<StickerTemplate> create(@Valid @RequestBody StickerTemplate template) {
        // 安全：强制覆盖受保护字段，防止客户端注入
        template.setId(null);
        template.setDeleted(0);
        template.setCreateTime(null);
        template.setUpdateTime(null);
        return Result.success(templateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新模板")
    @RequiresPermissions("sticker:template:edit")
    public Result<StickerTemplate> update(@PathVariable String id, @Valid @RequestBody StickerTemplate template) {
        // 安全：强制覆盖受保护字段
        template.setId(id);
        template.setDeleted(null);
        template.setCreateTime(null);
        template.setCreateBy(null);
        return Result.success(templateService.updateTemplate(id, template));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除模板")
    @RequiresPermissions("sticker:template:delete")
    public Result<?> delete(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return Result.success("删除成功");
    }

    @PostMapping("/{id}/set-default")
    @ApiOperation("设为默认模板")
    @RequiresPermissions("sticker:template:edit")
    public Result<?> setDefault(@PathVariable String id) {
        templateService.setDefault(id);
        return Result.success("设置成功");
    }

    @GetMapping("/default")
    @ApiOperation("获取默认模板")
    @RequiresPermissions("sticker:template:query")
    public Result<StickerTemplate> getDefault() {
        StickerTemplate tpl = templateService.getDefaultTemplate();
        if (tpl == null) {
            return Result.error("未配置默认模板");
        }
        return Result.success(tpl);
    }
}
