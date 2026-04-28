package com.bcsport.admin.dto;

import lombok.Data;

/**
 * ERP 客户查询数据传输对象
 */
@Data
public class ErpCustomerQueryDTO {

    private String customerCode;

    private String customerName;

    private String customerType;

    private String province;

    private String city;

    private Integer status;
}
