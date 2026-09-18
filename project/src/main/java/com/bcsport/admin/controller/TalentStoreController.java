package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.bi.TalentStore;
import com.bcsport.admin.service.TalentStoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 达人店铺管理（达人名称 + 伯俊ERP店仓编码/名称，手工维护）
 */
@RestController
@RequestMapping("/api/bi/talent-store")
@Api(tags = "达人店铺管理")
public class TalentStoreController {

    @Autowired
    private TalentStoreService talentStoreService;

    /**
     * 分页查询达人店铺列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询达人店铺")
    @RequiresPermissions("bi:talent-store:query")
    public Result<PageResult<TalentStore>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String talentName,
                                                @RequestParam(required = false) String storeCode,
                                                @RequestParam(required = false) String storeName) {
        return Result.success(talentStoreService.page(pageQuery, talentName, storeCode, storeName));
    }

    /**
     * 新增达人店铺绑定
     * body: { talentName, storeCode, storeName }
     */
    @PostMapping
    @ApiOperation("新增达人店铺")
    @OperLog(module = "达人店铺", operation = "新增达人店铺")
    @RequiresPermissions("bi:talent-store:add")
    public Result<Void> add(@RequestBody TalentStore body) {
        talentStoreService.add(body.getTalentName(), body.getStoreCode(), body.getStoreName());
        return Result.success(null);
    }

    /**
     * 编辑达人店铺绑定
     * body: { talentName, storeCode, storeName }
     */
    @PutMapping("/{id}")
    @ApiOperation("编辑达人店铺")
    @OperLog(module = "达人店铺", operation = "编辑达人店铺")
    @RequiresPermissions("bi:talent-store:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody TalentStore body) {
        talentStoreService.update(id, body.getTalentName(), body.getStoreCode(), body.getStoreName());
        return Result.success(null);
    }

    /**
     * 删除达人店铺绑定（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除达人店铺")
    @OperLog(module = "达人店铺", operation = "删除达人店铺")
    @RequiresPermissions("bi:talent-store:delete")
    public Result<Void> delete(@PathVariable Long id) {
        talentStoreService.delete(id);
        return Result.success(null);
    }
}
