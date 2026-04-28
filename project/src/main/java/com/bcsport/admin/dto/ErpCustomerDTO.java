package com.bcsport.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ERP 客户数据传输对象（新增或修改完
 */
@Data
public class ErpCustomerDTO {

    private String id;

    @NotBlank(message = "客户编码不能为空")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @NotBlank(message = "客户类型不能为空")
    private String customerType;

    private String province;

    private String city;

    private String district;

    private String address;

    private String contactPerson;

    private String contactPhone;

    private String email;

    private BigDecimal creditLimit;

    private Integer creditPeriod;

    @NotNull(message = "状态不能为null")
    private Integer status;

    private Integer sort;

    private String remark;
}
