package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.WarehouseDTO;
import com.bcsport.admin.entity.Warehouse;
import com.bcsport.admin.vo.WarehouseVO;

import java.util.List;

/**
 * 仓库服务接口
 */
public interface WarehouseService extends IService<Warehouse> {
    
    /**
     * 查询所有仓库列)
     */
    List<WarehouseVO> listWarehouses();
    
    /**
     * 根据ID获取仓库VO
     */
    WarehouseVO getWarehouseVOById(String id);

    /**
     * 新增仓库
     */
    boolean addWarehouse(WarehouseDTO warehouseDTO);

    /**
     * 修改仓库
     */
    boolean updateWarehouse(WarehouseDTO warehouseDTO);
    
    /**
     * 删除仓库（逻辑删除完
     */
    boolean deleteWarehouse(String id);
    
    /**
     * 查询所有启用状态的仓库
     */
    List<WarehouseVO> listEnabledWarehouses();
}
