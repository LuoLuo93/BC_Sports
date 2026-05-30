package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.NxcrmTagCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NxcrmTagCategoryMapper extends BaseMapper<NxcrmTagCategory> {
    void insertBatch(@Param("list") List<NxcrmTagCategory> list);
}
