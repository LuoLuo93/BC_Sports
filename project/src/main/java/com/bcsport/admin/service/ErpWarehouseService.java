package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpWarehouseDTO;
import com.bcsport.admin.dto.ErpWarehouseQueryDTO;
import com.bcsport.admin.entity.ErpWarehouse;
import com.bcsport.admin.vo.ErpWarehouseVO;

import java.util.List;

/**
 * ERP 仓库服务接口
 */
public interface ErpWarehouseService extends IService<ErpWarehouse> {

    /**
     * 分页查询仓库列表
     */
    PageResult<ErpWarehouseVO> pageErpWarehouses(PageQuery pageQuery, ErpWarehouseQueryDTO queryDTO);

    /**
     * 根据ID获取仓库VO
     */
    ErpWarehouseVO getErpWarehouseVOById(String id);

    /**
     * 新增仓库
     */
    boolean addErpWarehouse(ErpWarehouseDTO dto);

    /**
     * 修改仓库
     */
    boolean updateErpWarehouse(ErpWarehouseDTO dto);

    /**
     * 查询所有启用状态的仓库
     */
    List<ErpWarehouseVO> listEnabledErpWarehouses();
}
