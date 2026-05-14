package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("VX_CorpTag")
public class VxCorpTag {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tagId")
    private String tagId;

    @TableField("tagName")
    private String tagName;

    @TableField("groupId")
    private String groupId;

    @TableField("groupName")
    private String groupName;

    @TableField("sortOrder")
    private Integer sortOrder;

    private Integer deleted;

    @TableField("createTime")
    private LocalDateTime createTime;

    @TableField("updateTime")
    private LocalDateTime updateTime;
}
