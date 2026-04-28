package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 渠道性质实体类（树状结构）
 */
@Data
@TableName("bc_sports_sys_channel_nature")
public class ChannelNature implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("parent_id")
    private String parentId;

    @TableField("nature_name")
    private String natureName;

    @TableField("nature_code")
    private String natureCode;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Integer deleted;

    // 非数据库字段：子节点
    @TableField(exist = false)
    private List<ChannelNature> children = new ArrayList<>();
}
