package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrAttendanceSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrAttendanceSettingsMapper extends BaseMapper<IhrAttendanceSettings> {
    void insertBatch(@Param("list") List<IhrAttendanceSettings> list);
    void deleteAll();
}
