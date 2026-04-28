package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP 客户视图对象
 */
@Data
public class ErpCustomerVO {

    private String id;

    private String customerCode;

    private String customerName;

    private String customerType;

    private String customerTypeName;

    private String province;

    private String city;

    private String district;

    private String address;

    private String contactPerson;

    private String contactPhone;

    private String email;

    private BigDecimal creditLimit;

    private Integer creditPeriod;

    private Integer status;

    private String statusName;

    private Integer sort;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
