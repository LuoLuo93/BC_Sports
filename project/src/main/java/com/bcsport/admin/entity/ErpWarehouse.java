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
 * ERP 仓库实体类
 */
@Data
@TableName("bc_sports_sys_erp_warehouse")
@ApiModel(value = "ErpWarehouse对象", description = "ERP 仓库信息")
public class ErpWarehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("仓库ID")
    @TableId("id")
    private String id;

    @ApiModelProperty("仓库编码")
    @TableField("warehouse_code")
    private String warehouseCode;

    @ApiModelProperty("仓库名称")
    @TableField("warehouse_name")
    private String warehouseName;

    @ApiModelProperty("仓库类型：normal-普通仓，cold-冷链仓，bonded-保税仓")
    @TableField("warehouse_type")
    private String warehouseType;

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

    @ApiModelProperty("仓库负责人")
    @TableField("manager")
    private String manager;

    @ApiModelProperty("联系电话")
    @TableField("contact_phone")
    private String contactPhone;

    @ApiModelProperty("仓库容量")
    @TableField("capacity")
    private Integer capacity;

    @ApiModelProperty("已使用容量")
    @TableField("used_capacity")
    private Integer usedCapacity;

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

    @ApiModelProperty("创建时间")
    @TableField("create_by")
    private String createBy;

    @ApiModelProperty("更新时间")
    @TableField("update_by")
    private String updateBy;

    @ApiModelProperty("删除标记：0-未删除，1-已删除")
    @TableField("deleted")
    private Integer deleted;
}
