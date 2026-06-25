package com.bcsport.admin.ydklmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ydkl.YdWeather;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface YdWeatherMapper extends BaseMapper<YdWeather> {
    void deleteTodayData();

    void deleteByDate(@Param("date") String date);

    void insertBatch(@Param("list") List<YdWeather> list);
}
