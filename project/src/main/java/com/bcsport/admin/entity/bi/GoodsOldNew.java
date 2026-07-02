package com.bcsport.admin.entity.bi;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 货品资料（对应 BC_SPORTS_BI_GOODS_OLDNEW）
 */
@Data
@TableName("BC_SPORTS_BI_GOODS_OLDNEW")
public class GoodsOldNew implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 品牌 */
    private String brand;

    /** 货号 */
    private String articleNo;

    /** 产品季 */
    private String season;

    /** 货品分类 */
    private String category;

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
