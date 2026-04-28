package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * ERP 仓库视图对象
 */
@Data
public class ErpWarehouseVO {

    private String id;

    private String warehouseCode;

    private String warehouseName;

    private String warehouseType;

    private String warehouseTypeName;

    private String province;

    private String city;

    private String district;

    private String address;

    private String manager;

    private String contactPhone;

    private Integer capacity;

    private Integer usedCapacity;

    private Integer status;

    private String statusName;

    private Integer sort;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
