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
 * 渠道类型实体类（树状结构）
 */
@Data
@TableName("bc_sports_sys_channel_type")
public class ChannelType implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("parent_id")
    private String parentId;

    @TableField("type_name")
    private String typeName;

    @TableField("type_code")
    private String typeCode;

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
    private List<ChannelType> children = new ArrayList<>();
}
