package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP 客户实体类
 */
@Data
@TableName("bc_sports_sys_erp_customer")
@ApiModel(value = "ErpCustomer对象", description = "ERP 客户信息")
public class ErpCustomer implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("客户ID")
    @TableId("id")
    private String id;

    @ApiModelProperty("客户编码")
    @TableField("customer_code")
    private String customerCode;

    @ApiModelProperty("客户名称")
    @TableField("customer_name")
    private String customerName;

    @ApiModelProperty("客户类型：enterprise-企业客户，individual-个人客户，dealer-经销商")
    @TableField("customer_type")
    private String customerType;

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

    @ApiModelProperty("电子邮箱")
    @TableField("email")
    private String email;

    @ApiModelProperty("信用额度")
    @TableField("credit_limit")
    private BigDecimal creditLimit;

    @ApiModelProperty("信用账期（天）")
    @TableField("credit_period")
    private Integer creditPeriod;

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
