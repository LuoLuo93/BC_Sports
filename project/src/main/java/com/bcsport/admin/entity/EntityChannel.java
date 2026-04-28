package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体渠道配置实体类
 * 用于管理店铺、仓库、客户的渠道属性配置）
 */
@Data
@TableName("bc_sports_sys_entity_channel")
public class EntityChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("external_id")
    private String externalId;

    @TableField("brand_id")
    private String brandId;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_name")
    private String entityName;

    @TableField("channel_type_id")
    private String channelTypeId;

    @TableField("channel_def_id")
    private String channelDefId;

    @TableField("channel_nature_id")
    private String channelNatureId;

    @TableField("business_type_id")
    private String businessTypeId;

    @TableField("region_level1_id")
    private String regionLevel1Id;

    @TableField("region_level2_id")
    private String regionLevel2Id;

    @TableField("status")
    private Integer status;

    @TableField("sort")
    private Integer sort;

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
}
