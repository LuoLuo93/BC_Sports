package com.bcsport.admin.vo;

import lombok.Data;

/**
 * 运动积分排名视图对象（移动端免登录榜单页专用，只暴露展示必需字段）
 */
@Data
public class SportPointsRankVO {

    private Long id;

    /** 运动员姓名（BC_SPORTS_BCP_SPORT_POINTS.sporter） */
    private String name;

    private Long points;

    /** 全榜绝对名次（积分降序、同分按姓名稳定排序后的位次） */
    private Long rank;
}
