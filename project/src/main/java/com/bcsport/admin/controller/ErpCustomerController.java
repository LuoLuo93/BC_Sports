package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.erpmapper.BjerpCustomerMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ERP 客户管理控制器（数据源：bjerp WMS_CUSTOMER）
 */
@RestController
@RequestMapping("/api/erp-customer")
@Api(tags = "ERP 客户管理")
public class ErpCustomerController {

    @Autowired
    private BjerpCustomerMapper bjerpCustomerMapper;

    /**
     * 分页查询客户列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询客户列表")
    @RequiresPermissions("bi:erpCustomer:query")
    public Result<PageResult<Map<String, Object>>> page(PageQuery pageQuery,
                                                         @RequestParam(required = false) String code,
                                                         @RequestParam(required = false) String name) {
        int safePageSize = Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10, 500);
        int safePageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        long offset = (long) (safePageNum - 1) * safePageSize;

        long total = bjerpCustomerMapper.countCustomers(code, name);
        List<Map<String, Object>> list = bjerpCustomerMapper.searchCustomers(code, name, offset, safePageSize);

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
     * 查询所有客户（下拉选择用）
     */
    @GetMapping("/list-all")
    @ApiOperation("查询所有客户")
    @RequiresPermissions("bi:erpCustomer:query")
    public Result<List<Map<String, Object>>> listAll() {
        return Result.success(bjerpCustomerMapper.listAllCustomers());
    }
}
