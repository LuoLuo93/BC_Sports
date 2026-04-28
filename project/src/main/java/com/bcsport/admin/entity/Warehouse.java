package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 仓库实体类
 */
@Data
@TableName("bc_sports_sys_warehouse")
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("warehouse_name")
    private String warehouseName;

    @TableField("warehouse_code")
    private String warehouseCode;

    @TableField("contact_person")
    private String contactPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("address")
    private String address;

    @TableField("description")
    private String description;

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
