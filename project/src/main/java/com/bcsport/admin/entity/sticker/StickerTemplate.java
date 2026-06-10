package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sticker_template")
public class StickerTemplate {
    @TableId("id")
    private String id;

    @TableField("template_name")
    private String templateName;

    @TableField("label_width")
    private BigDecimal labelWidth;

    @TableField("label_height")
    private BigDecimal labelHeight;

    @TableField("dpi")
    private Integer dpi;

    /** 模板元素 JSON (CLOB) */
    @TableField(value = "template_data", jdbcType = JdbcType.CLOB)
    private String templateData;

    @TableField("is_default")
    private Integer isDefault;

    @TableField("status")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Integer deleted;
}
