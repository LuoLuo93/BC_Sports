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

    /**
     * 基于表内现有最大数字 id 生成下一个 id（不依赖序列，避免序列与表数据不同步导致主键冲突）。
     * 仅对纯数字 id 求最大值，非数字 id 不参与计算。
     */
    @Select("SELECT NVL(MAX(TO_NUMBER(id)), 0) + 1 FROM bc_sports_sys_brand WHERE REGEXP_LIKE(id, '^\\d+$')")
    Long selectMaxId();
}
