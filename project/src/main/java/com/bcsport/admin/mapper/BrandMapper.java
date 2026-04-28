package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.Brand;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Select;

/**
 * 品牌Mapper接口
 */
@Mapper
public interface BrandMapper extends BaseMapper<Brand> {
    
    @Select("SELECT bc_sports_seq_brand.NEXTVAL FROM DUAL")
    Long selectNextId();
}
