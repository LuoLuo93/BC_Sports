package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.NxcrmTagValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NxcrmTagValueMapper extends BaseMapper<NxcrmTagValue> {
    void insertBatch(@Param("list") List<NxcrmTagValue> list);
}
