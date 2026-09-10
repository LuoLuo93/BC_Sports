package com.bcsport.admin.entity.bcp;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BC好玩家运动积分（对应 BC_SPORTS_BCP_SPORT_POINTS）
 */
@Data
@TableName("BC_SPORTS_BCP_SPORT_POINTS")
public class BcpSportPoints implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 运动员/玩家名（导入去重键，未删除记录内唯一） */
    private String sporter;

    /** 积分 */
    private Long points;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
