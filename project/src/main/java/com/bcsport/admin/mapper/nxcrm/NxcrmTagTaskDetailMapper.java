package com.bcsport.admin.mapper.nxcrm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.nxcrm.NxcrmTagTaskDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NxcrmTagTaskDetailMapper extends BaseMapper<NxcrmTagTaskDetail> {

    int insertBatch(@Param("list") List<NxcrmTagTaskDetail> list);
}
