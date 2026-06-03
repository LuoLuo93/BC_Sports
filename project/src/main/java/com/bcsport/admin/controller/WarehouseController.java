package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.WarehouseDTO;
import com.bcsport.admin.service.WarehouseService;
import com.bcsport.admin.vo.WarehouseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/warehouse")
@Api(tags = "仓库管理")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    /**
     * 获取仓库列表
     */
    @GetMapping("/list")
    @ApiOperation("获取仓库列表")
    @RequiresPermissions("warehouse:query")
    public Result<List<WarehouseVO>> list() {
        log.debug("查询仓库列表");
        List<WarehouseVO> warehouses = warehouseService.listWarehouses();
        log.debug("查询仓库列表完成，数量：{}", warehouses.size());
        return Result.success(warehouses);
    }

    /**
     * 获取启用的仓库列表（下拉选择用）
     */
    @GetMapping("/enabled")
    @ApiOperation("获取启用的仓库列表")
    public Result<List<WarehouseVO>> listEnabled() {
        log.debug("查询启用的仓库列表");
        List<WarehouseVO> warehouses = warehouseService.listEnabledWarehouses();
        log.debug("查询启用的仓库列表完成，数量：{}", warehouses.size());
        return Result.success(warehouses);
    }

    /**
     * 根据ID查询仓库
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询仓库")
    @RequiresPermissions("warehouse:query")
    public Result<WarehouseVO> getById(@PathVariable String id) {
        log.debug("查询仓库详情，ID：{}", id);
        WarehouseVO warehouse = warehouseService.getWarehouseVOById(id);
        if (warehouse == null) {
            log.warn("仓库不存在，ID：{}", id);
            return Result.notFound("仓库不存在");
        }
        return Result.success(warehouse);
    }

    /**
     * 新增仓库
     */
    @PostMapping
    @ApiOperation("新增仓库")
    @RequiresPermissions("warehouse:add")
    public Result<?> add(@Validated @RequestBody WarehouseDTO warehouseDTO) {
        log.info("新增仓库请求，仓库名称：{}，编码：{}", warehouseDTO.getWarehouseName(), warehouseDTO.getWarehouseCode());
        warehouseService.addWarehouse(warehouseDTO);
        return Result.success("新增仓库成功", null);
    }

    /**
     * 更新仓库
     */
    @PutMapping("/{id}")
    @ApiOperation("更新仓库")
    @RequiresPermissions("warehouse:edit")
    public Result<?> update(@PathVariable String id, @Validated @RequestBody WarehouseDTO warehouseDTO) {
        log.info("更新仓库请求，ID：{}，仓库名称：{}", id, warehouseDTO.getWarehouseName());
        warehouseDTO.setId(id);
        warehouseService.updateWarehouse(warehouseDTO);
        return Result.success("更新仓库成功", null);
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除仓库")
    @RequiresPermissions("warehouse:delete")
    public Result<?> delete(@PathVariable String id) {
        log.info("删除仓库请求，ID：{}", id);
        warehouseService.deleteWarehouse(id);
        return Result.success("删除仓库成功", null);
    }
}
