package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrAttendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrAttendanceMapper extends BaseMapper<IhrAttendance> {
    void insertBatch(@Param("list") List<IhrAttendance> list);
    void deleteAll();
}
