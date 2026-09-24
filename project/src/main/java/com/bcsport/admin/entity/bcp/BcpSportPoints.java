package com.bcsport.admin.entity.bcp;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
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

    /** 积分（整数，导入/编辑小数自动四舍五入取整） */
    private BigDecimal points;

    /** 自定义头像URL（管理员代传，/images/avatar/xxx；空则移动端回退动物emoji。Excel 重导入不覆盖） */
    private String avatarUrl;

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
