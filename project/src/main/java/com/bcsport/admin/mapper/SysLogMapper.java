package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bcsport.admin.dto.SysLogQueryDTO;
import com.bcsport.admin.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    IPage<SysLog> selectLogPage(IPage<SysLog> page, @Param("query") SysLogQueryDTO query);

    int deleteBeforeDays(@Param("days") int days);
}
