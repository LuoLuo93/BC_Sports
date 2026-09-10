package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bcp.BcpSportPoints;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BcpSportPointsMapper extends BaseMapper<BcpSportPoints> {

    /**
     * 批量 MERGE upsert（sporter 去重，一名运动员只保留一行积分）
     */
    void mergeBatch(@Param("list") List<BcpSportPoints> list);
}
