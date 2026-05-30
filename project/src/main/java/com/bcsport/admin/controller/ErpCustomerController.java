package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ErpCustomerDTO;
import com.bcsport.admin.dto.ErpCustomerQueryDTO;
import com.bcsport.admin.entity.ErpCustomer;
import com.bcsport.admin.mapper.ErpCustomerMapper;
import com.bcsport.admin.service.ErpCustomerService;
import com.bcsport.admin.vo.ErpCustomerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * ERP 客户管理控制器
 */
@RestController
@RequestMapping("/api/erp-customer")
@Api(tags = "ERP 客户管理")
public class ErpCustomerController {

    @Autowired
    private ErpCustomerService erpCustomerService;

    @Autowired
    private ErpCustomerMapper erpCustomerMapper;

    /**
     * 分页查询客户列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询客户列表")
    @RequiresPermissions("bi:erpCustomer:query")
    public Result<PageResult<ErpCustomerVO>> pageErpCustomers(PageQuery pageQuery, ErpCustomerQueryDTO queryDTO) {
        PageResult<ErpCustomerVO> pageResult = erpCustomerService.pageErpCustomers(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询客户
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询客户")
    @RequiresPermissions("bi:erpCustomer:query")
    public Result<ErpCustomerVO> getById(@PathVariable String id) {
        ErpCustomerVO vo = erpCustomerService.getErpCustomerVOById(id);
        if (vo == null) {
            return Result.notFound("客户不存在");
        }
        return Result.success(vo);
    }

    /**
     * 新增客户
     */
    @PostMapping
    @ApiOperation("新增客户")
    @RequiresPermissions("bi:erpCustomer:add")
    public Result<?> add(@Valid @RequestBody ErpCustomerDTO dto) {
        // 使用数据库序列生成递增 ID
        if (dto.getId() == null || dto.getId().isEmpty()) {
            dto.setId(String.valueOf(erpCustomerMapper.selectNextId()));
        }
        boolean success = erpCustomerService.addErpCustomer(dto);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 修改客户
     */
    @PutMapping("/{id}")
    @ApiOperation("修改客户")
    @RequiresPermissions("bi:erpCustomer:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody ErpCustomerDTO dto) {
        dto.setId(id);
        boolean success = erpCustomerService.updateErpCustomer(dto);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 删除客户（逻辑删除完
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除客户")
    @RequiresPermissions("bi:erpCustomer:delete")
    public Result<?> delete(@PathVariable String id) {
        // 使用 MyBatis-Plus 的逻辑删除
        boolean success = erpCustomerService.removeById(id);
        return success ? Result.success("客户已被成功移除") : Result.error("未找到对应客户，移除失败");
    }

    /**
     * 查询所有启用状态的客户
     */
    @GetMapping("/list-enabled")
    @ApiOperation("查询所有启用状态的客户")
    @RequiresPermissions("bi:erpCustomer:query")
    public Result<List<ErpCustomerVO>> listEnabled() {
        List<ErpCustomerVO> list = erpCustomerService.listEnabledErpCustomers();
        return Result.success(list);
    }
}
