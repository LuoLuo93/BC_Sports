package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.bi.StoreWhitelist;
import com.bcsport.admin.service.StoreWhitelistService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 自提店铺白名单管理（伯俊ERP店仓编码/名称，手工维护）
 * 反向白名单：数仓 SP_FILL_ODS_SALES_MAIN ④小程序更新时，订单店铺命中本表则不改写店铺两列
 */
@RestController
@RequestMapping("/api/bi/store-whitelist")
@Api(tags = "自提店铺白名单管理")
public class StoreWhitelistController {

    @Autowired
    private StoreWhitelistService storeWhitelistService;

    /**
     * 分页查询白名单店铺列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询自提店铺白名单")
    @RequiresPermissions("bi:store-whitelist:query")
    public Result<PageResult<StoreWhitelist>> page(PageQuery pageQuery,
                                                   @RequestParam(required = false) String storeCode,
                                                   @RequestParam(required = false) String storeName) {
        return Result.success(storeWhitelistService.page(pageQuery, storeCode, storeName));
    }

    /**
     * 新增白名单店铺
     * body: { storeCode, storeName }
     */
    @PostMapping
    @ApiOperation("新增自提店铺")
    @OperLog(module = "自提店铺白名单", operation = "新增自提店铺")
    @RequiresPermissions("bi:store-whitelist:add")
    public Result<Void> add(@RequestBody StoreWhitelist body) {
        storeWhitelistService.add(body.getStoreCode(), body.getStoreName());
        return Result.success(null);
    }

    /**
     * 编辑白名单店铺
     * body: { storeCode, storeName }
     */
    @PutMapping("/{id}")
    @ApiOperation("编辑自提店铺")
    @OperLog(module = "自提店铺白名单", operation = "编辑自提店铺")
    @RequiresPermissions("bi:store-whitelist:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody StoreWhitelist body) {
        storeWhitelistService.update(id, body.getStoreCode(), body.getStoreName());
        return Result.success(null);
    }

    /**
     * 删除白名单店铺（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除自提店铺")
    @OperLog(module = "自提店铺白名单", operation = "删除自提店铺")
    @RequiresPermissions("bi:store-whitelist:delete")
    public Result<Void> delete(@PathVariable Long id) {
        storeWhitelistService.delete(id);
        return Result.success(null);
    }
}
