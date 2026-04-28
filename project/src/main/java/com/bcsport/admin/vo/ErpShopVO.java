package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * ERP 店铺视图对象
 */
@Data
public class ErpShopVO {

    private String id;

    private String shopCode;

    private String shopName;

    private String shopType;

    private String shopTypeName;

    private String province;

    private String city;

    private String district;

    private String address;

    private String contactPerson;

    private String contactPhone;

    private Integer status;

    private String statusName;

    private Integer sort;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
