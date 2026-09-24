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
     * 移动端榜单：无关键字只返回前 N 名；有关键字全库按姓名模糊搜索（带 ROW_NUMBER 绝对名次，
     * 可搜到 N 名以外的人，命中过多截前 500 条）
     */
    List<SportPointsRankVO> selectRank(@Param("limit") int limit, @Param("keyword") String keyword);

    /**
     * 全表统计：参与人数 / 累计积分
     */
    Map<String, Object> selectRankStats();
}
