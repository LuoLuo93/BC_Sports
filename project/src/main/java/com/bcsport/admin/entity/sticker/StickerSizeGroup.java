package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 贴纸本地尺码组(按品牌+类别隔离)
 */
@Data
@TableName("sticker_size_group")
public class StickerSizeGroup {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("group_code")
    private String groupCode;

    @TableField("group_name")
    private String groupName;

    @TableField("brand_id")
    private String brandId;

    @TableField("brand_name")
    private String brandName;

    @TableField("kind_id")
    private String kindId;

    @TableField("kind_name")
    private String kindName;

    /** 1启用 0停用 */
    @TableField("status")
    private Integer status;

    @TableField("sort")
    private Integer sort;

    @TableField("remark")
    private String remark;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 尺码明细(非持久化,一次性提交/返回) */
    @TableField(exist = false)
    private List<StickerSize> sizes;
}
