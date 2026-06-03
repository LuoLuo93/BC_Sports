package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.erpmapper.BjerpStoreMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ERP 店仓管理控制器（数据源：bjerp C_STORE）
 */
@RestController
@RequestMapping("/api/erp-store")
@Api(tags = "ERP 店仓管理")
public class ErpStoreController {

    @Autowired
    private BjerpStoreMapper bjerpStoreMapper;

    /**
     * 分页查询店仓列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询店仓列表")
    @RequiresPermissions("bi:erpStore:query")
    public Result<PageResult<Map<String, Object>>> page(PageQuery pageQuery,
                                                         @RequestParam(required = false) String code,
                                                         @RequestParam(required = false) String name) {
        int safePageSize = Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10, 500);
        int safePageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        long offset = (long) (safePageNum - 1) * safePageSize;

        long total = bjerpStoreMapper.countStores(code, name);
        List<Map<String, Object>> list = bjerpStoreMapper.searchStores(code, name, offset, safePageSize);

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
     * 查询所有店仓（下拉选择用）
     */
    @GetMapping("/list-all")
    @ApiOperation("查询所有店仓")
    @RequiresPermissions("bi:erpStore:query")
    public Result<List<Map<String, Object>>> listAll() {
        return Result.success(bjerpStoreMapper.listAllStores());
    }
}
