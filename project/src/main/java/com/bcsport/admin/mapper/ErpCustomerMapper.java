package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ErpCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * ERP 客户 Mapper 接口
 */
@Mapper
public interface ErpCustomerMapper extends BaseMapper<ErpCustomer> {

    @Select("SELECT bc_sports_seq_erp_customer.NEXTVAL FROM DUAL")
    Long selectNextId();
}
