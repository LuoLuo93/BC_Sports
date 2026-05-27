package com.bcsport.admin.mapper.nxcrm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.nxcrm.NxcrmMemberTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NxcrmMemberTagMapper extends BaseMapper<NxcrmMemberTag> {

    int insertBatch(@Param("list") List<NxcrmMemberTag> list);
}
