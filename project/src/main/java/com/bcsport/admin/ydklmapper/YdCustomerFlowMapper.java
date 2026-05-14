package com.bcsport.admin.ydklmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ydkl.YdCustomerFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface YdCustomerFlowMapper extends BaseMapper<YdCustomerFlow> {
    void deleteTodayData();

    void insertBatch(@Param("list") List<YdCustomerFlow> list);
}
