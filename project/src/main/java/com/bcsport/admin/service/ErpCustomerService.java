package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpCustomerDTO;
import com.bcsport.admin.dto.ErpCustomerQueryDTO;
import com.bcsport.admin.entity.ErpCustomer;
import com.bcsport.admin.vo.ErpCustomerVO;

import java.util.List;

/**
 * ERP 客户服务接口
 */
public interface ErpCustomerService extends IService<ErpCustomer> {

    /**
     * 分页查询客户列表
     */
    PageResult<ErpCustomerVO> pageErpCustomers(PageQuery pageQuery, ErpCustomerQueryDTO queryDTO);

    /**
     * 根据ID获取客户VO
     */
    ErpCustomerVO getErpCustomerVOById(String id);

    /**
     * 新增客户
     */
    boolean addErpCustomer(ErpCustomerDTO dto);

    /**
     * 修改客户
     */
    boolean updateErpCustomer(ErpCustomerDTO dto);

    /**
     * 查询所有启用状态的客户
     */
    List<ErpCustomerVO> listEnabledErpCustomers();
}
