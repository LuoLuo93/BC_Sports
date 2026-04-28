package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 品牌实体类 */
@Data
@TableName("bc_sports_sys_brand")
@ApiModel(value = "Brand对象", description = "品牌信息")
public class Brand implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("品牌ID")
    @TableId("id")
    private String id;

    @ApiModelProperty("品牌名称")
    @TableField("brand_name")
    private String brandName;

    @ApiModelProperty("品牌LOGO")
    @TableField("brand_logo")
    private String brandLogo;

    @ApiModelProperty("描述")
    @TableField("description")
    private String description;

    @ApiModelProperty("状态（1启用 0禁用）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("排序")
    @TableField("sort")
    private Integer sort;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Date updateTime;

    @ApiModelProperty("创建人")
    @TableField("create_by")
    private String createBy;

    @ApiModelProperty("更新人")
    @TableField("update_by")
    private String updateBy;

    @TableField("deleted")
    private Integer deleted;
}
