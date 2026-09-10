package com.bcsport.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 移动端榜单整体数据：列表 + 全表统计（头部指标）
 */
@Data
public class SportPointsBoardVO {

    /** 榜单/搜索结果（无关键字=前N名；有关键字=姓名匹配，均带绝对名次） */
    private List<SportPointsRankVO> list;

    /** 参与人数（无关键字=全表真实人数；有关键字=匹配条数） */
    private Long participants;

    /** 累计积分（与 participants 同口径） */
    private Long totalPoints;
}
