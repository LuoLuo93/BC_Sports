package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ErpWarehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * ERP 仓库 Mapper 接口
 */
@Mapper
public interface ErpWarehouseMapper extends BaseMapper<ErpWarehouse> {

    @Select("SELECT bc_sports_seq_erp_warehouse.NEXTVAL FROM DUAL")
    Long selectNextId();
}
