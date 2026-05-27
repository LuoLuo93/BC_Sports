package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("NXCRM_TAG_VALUE")
public class NxcrmTagValue {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField("TAG_CODE")
    private String tagCode;

    @TableField("TAG_VALUE_CODE")
    private String tagValueCode;

    @TableField("TAG_VALUE_NAME")
    private String tagValueName;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("DISPLAY_ORDER")
    private Integer displayOrder;

    @TableField("SYNC_TIME")
    private LocalDateTime syncTime;
}
