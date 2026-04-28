package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ErpShop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * ERP 店铺 Mapper 接口
 */
@Mapper
public interface ErpShopMapper extends BaseMapper<ErpShop> {

    @Select("SELECT bc_sports_seq_erp_shop.NEXTVAL FROM DUAL")
    Long selectNextId();
}
