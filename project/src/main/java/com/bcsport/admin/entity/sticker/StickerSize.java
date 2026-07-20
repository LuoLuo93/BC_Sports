package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 贴纸本地尺码明细
 */
@Data
@TableName("sticker_sizes")
public class StickerSize {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("group_id")
    private String groupId;

    @TableField("size_code")
    private String sizeCode;

    @TableField("size_name")
    private String sizeName;

    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
