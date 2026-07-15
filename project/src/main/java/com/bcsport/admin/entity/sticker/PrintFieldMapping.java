package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("print_field_mapping")
public class PrintFieldMapping {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("template_id")
    private String templateId;

    @TableField("db_field")
    private String dbField;

    @TableField("template_field")
    private String templateField;

    @TableField("field_format")
    private String fieldFormat;

    @TableField("default_value")
    private String defaultValue;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
