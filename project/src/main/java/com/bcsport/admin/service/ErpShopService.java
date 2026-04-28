package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpShopDTO;
import com.bcsport.admin.dto.ErpShopQueryDTO;
import com.bcsport.admin.entity.ErpShop;
import com.bcsport.admin.vo.ErpShopVO;

import java.util.List;

/**
 * ERP 店铺服务接口
 */
public interface ErpShopService extends IService<ErpShop> {

    /**
     * 分页查询店铺列表
     */
    PageResult<ErpShopVO> pageErpShops(PageQuery pageQuery, ErpShopQueryDTO queryDTO);

    /**
     * 根据ID获取店铺VO
     */
    ErpShopVO getErpShopVOById(String id);

    /**
     * 新增店铺
     */
    boolean addErpShop(ErpShopDTO dto);

    /**
     * 修改店铺
     */
    boolean updateErpShop(ErpShopDTO dto);

    /**
     * 查询所有启用状态的店铺
     */
    List<ErpShopVO> listEnabledErpShops();
}
