package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sticker_brand_template_match")
public class BrandTemplateMatch {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("brand_id")
    private String brandId;

    @TableField("brand_name")
    private String brandName;

    @TableField("kind_id")
    private String kindId;

    @TableField("kind_name")
    private String kindName;

    @TableField("template_id")
    private String templateId;

    @TableField("template_name")
    private String templateName;

    @TableField("printer_name")
    private String printerName;

    @TableField("remark")
    private String remark;

    @TableField("is_active")
    private Integer isActive;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
