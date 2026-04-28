package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 仓库Mapper接口
 */
@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
    
    @Select("SELECT bc_sports_seq_warehouse.NEXTVAL FROM DUAL")
    Long selectNextId();
}
