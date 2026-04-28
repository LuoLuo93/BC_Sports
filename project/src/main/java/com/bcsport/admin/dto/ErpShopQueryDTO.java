package com.bcsport.admin.dto;

import lombok.Data;

/**
 * ERP 店铺查询数据传输对象
 */
@Data
public class ErpShopQueryDTO {

    private String shopCode;

    private String shopName;

    private String shopType;

    private String province;

    private String city;

    private Integer status;
}
