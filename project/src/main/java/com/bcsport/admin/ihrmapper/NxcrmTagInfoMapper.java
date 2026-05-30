package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.NxcrmTagInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NxcrmTagInfoMapper extends BaseMapper<NxcrmTagInfo> {
    void insertBatch(@Param("list") List<NxcrmTagInfo> list);
}
