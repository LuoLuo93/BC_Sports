package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bcp.BcpSportPoints;
import com.bcsport.admin.vo.SportPointsRankVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BcpSportPointsMapper extends BaseMapper<BcpSportPoints> {

    /**
     * 批量 MERGE upsert（sporter 去重，一名运动员只保留一行积分）
     */
    void mergeBatch(@Param("list") List<BcpSportPoints> list);

    /**
     * 移动端榜单：无关键字返回前 N 名；有关键字按姓名模糊匹配全表（均带 ROW_NUMBER 绝对名次）
     */
    List<SportPointsRankVO> selectRank(@Param("limit") int limit, @Param("keyword") String keyword);

    /**
     * 全表统计：参与人数 / 累计积分
     */
    Map<String, Object> selectRankStats();
}
