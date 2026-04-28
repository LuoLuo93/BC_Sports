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
 * ERP 店铺实体类
 */
@Data
@TableName("bc_sports_sys_erp_shop")
@ApiModel(value = "ErpShop对象", description = "ERP 店铺信息")
public class ErpShop implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("店铺ID")
    @TableId("id")
    private String id;

    @ApiModelProperty("店铺编码")
    @TableField("shop_code")
    private String shopCode;

    @ApiModelProperty("店铺名称")
    @TableField("shop_name")
    private String shopName;

    @ApiModelProperty("店铺类型：online-线上，offline-线下")
    @TableField("shop_type")
    private String shopType;

    @ApiModelProperty("省份")
    @TableField("province")
    private String province;

    @ApiModelProperty("城市")
    @TableField("city")
    private String city;

    @ApiModelProperty("区县")
    @TableField("district")
    private String district;

    @ApiModelProperty("详细地址")
    @TableField("address")
    private String address;

    @ApiModelProperty("联系人")
    @TableField("contact_person")
    private String contactPerson;

    @ApiModelProperty("联系电话")
    @TableField("contact_phone")
    private String contactPhone;

    @ApiModelProperty("状态：1-启用，0-禁用")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("排序")
    @TableField("sort")
    private Integer sort;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

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

    @ApiModelProperty("删除标记：0-未删除，1-已删除")
    @TableField("deleted")
    private Integer deleted;
}
