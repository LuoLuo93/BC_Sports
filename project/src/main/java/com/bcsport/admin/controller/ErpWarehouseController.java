package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ErpWarehouseDTO;
import com.bcsport.admin.dto.ErpWarehouseQueryDTO;
import com.bcsport.admin.entity.ErpWarehouse;
import com.bcsport.admin.mapper.ErpWarehouseMapper;
import com.bcsport.admin.service.ErpWarehouseService;
import com.bcsport.admin.vo.ErpWarehouseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * ERP 仓库管理控制器
 */
@RestController
@RequestMapping("/api/erp-warehouse")
@Api(tags = "ERP 仓库管理")
public class ErpWarehouseController {

    @Autowired
    private ErpWarehouseService erpWarehouseService;

    @Autowired
    private ErpWarehouseMapper erpWarehouseMapper;

    /**
     * 分页查询仓库列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询仓库列表")
    @RequiresPermissions("bi:erpWarehouse:query")
    public Result<PageResult<ErpWarehouseVO>> pageErpWarehouses(PageQuery pageQuery, ErpWarehouseQueryDTO queryDTO) {
        PageResult<ErpWarehouseVO> pageResult = erpWarehouseService.pageErpWarehouses(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询仓库
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询仓库")
    @RequiresPermissions("bi:erpWarehouse:query")
    public Result<ErpWarehouseVO> getById(@PathVariable String id) {
        ErpWarehouseVO vo = erpWarehouseService.getErpWarehouseVOById(id);
        if (vo == null) {
            return Result.notFound("仓库不存在");
        }
        return Result.success(vo);
    }

    /**
     * 新增仓库
     */
    @PostMapping
    @ApiOperation("新增仓库")
    @RequiresPermissions("bi:erpWarehouse:add")
    public Result<?> add(@Valid @RequestBody ErpWarehouseDTO dto) {
        // 使用数据库序列生成递增 ID
        if (dto.getId() == null || dto.getId().isEmpty()) {
            dto.setId(String.valueOf(erpWarehouseMapper.selectNextId()));
        }
        boolean success = erpWarehouseService.addErpWarehouse(dto);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 修改仓库
     */
    @PutMapping("/{id}")
    @ApiOperation("修改仓库")
    @RequiresPermissions("bi:erpWarehouse:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody ErpWarehouseDTO dto) {
        dto.setId(id);
        boolean success = erpWarehouseService.updateErpWarehouse(dto);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 删除仓库（逻辑删除完成）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除仓库")
    @RequiresPermissions("bi:erpWarehouse:delete")
    public Result<?> delete(@PathVariable String id) {
        // 使用 MyBatis-Plus 的逻辑删除
        boolean success = erpWarehouseService.removeById(id);
        return success ? Result.success("仓库已被成功移除") : Result.error("未找到对应仓库，移除失败");
    }

    /**
     * 查询所有启用状态的仓库
     */
    @GetMapping("/list-enabled")
    @ApiOperation("查询所有启用状态的仓库")
    @RequiresPermissions("bi:erpWarehouse:query")
    public Result<List<ErpWarehouseVO>> listEnabled() {
        List<ErpWarehouseVO> list = erpWarehouseService.listEnabledErpWarehouses();
        return Result.success(list);
    }
}
