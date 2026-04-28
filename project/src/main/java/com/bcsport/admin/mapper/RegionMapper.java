package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.Region;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegionMapper extends BaseMapper<Region> {
    @Select("SELECT bc_sports_seq_region.NEXTVAL FROM dual")
    Long selectNextId();
}
