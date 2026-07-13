package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.erpmapper.LzCustomerMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 揽众客户押金资料管理（数据源：bjerp LZCUSTOMERINFOR）
 */
@RestController
@RequestMapping("/api/lz-customer")
@Api(tags = "揽众客户押金资料")
public class LzCustomerController {

    @Autowired
    private LzCustomerMapper lzCustomerMapper;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @ApiOperation("分页查询揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:query")
    public Result<PageResult<Map<String, Object>>> page(PageQuery pageQuery,
                                                         @RequestParam(required = false) String shopCode,
                                                         @RequestParam(required = false) String shopName,
                                                         @RequestParam(required = false) String shopBoss) {
        int safePageSize = Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10, 500);
        int safePageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        long offset = (long) (safePageNum - 1) * safePageSize;

        long total = lzCustomerMapper.countLzCustomers(shopCode, shopName, shopBoss);
        List<Map<String, Object>> list = lzCustomerMapper.searchLzCustomers(shopCode, shopName, shopBoss, offset, safePageSize);

        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setPageNum((long) safePageNum);
        pageResult.setPageSize((long) safePageSize);
        pageResult.setTotal(total);
        pageResult.setPages((total + safePageSize - 1) / safePageSize);
        pageResult.setRecords(list);
        pageResult.setHasPrevious(safePageNum > 1);
        pageResult.setHasNext(safePageNum < pageResult.getPages());

        return Result.success(pageResult);
    }

    /**
     * 查询单条
     */
    @GetMapping("/{id}")
    @ApiOperation("查询单条揽众客户资料")
    @RequiresPermissions("erp:lzCustomer:query")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> data = lzCustomerMapper.getLzCustomerById(id);
        if (data == null) {
            return Result.error("数据不存在");
        }
        return Result.success(data);
    }

    /**
     * 新增
     */
    @PostMapping
    @ApiOperation("新增揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("shopCode", body.get("shopCode"));
        map.put("shopName", body.get("shopName"));
        map.put("shopBoss", body.get("shopBoss"));
        map.put("fundingLimit", body.get("fundingLimit"));
        map.put("fundingRatio", body.get("fundingRatio"));
        lzCustomerMapper.insertLzCustomer(map);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/{id}")
    @ApiOperation("修改揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("shopCode", body.get("shopCode"));
        map.put("shopName", body.get("shopName"));
        map.put("shopBoss", body.get("shopBoss"));
        map.put("fundingLimit", body.get("fundingLimit"));
        map.put("fundingRatio", body.get("fundingRatio"));
        lzCustomerMapper.updateLzCustomer(map);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除揽众客户押金资料")
    @RequiresPermissions("erp:lzCustomer:delete")
    public Result<Void> delete(@PathVariable Long id) {
        lzCustomerMapper.deleteLzCustomer(id);
        return Result.success();
    }
}
