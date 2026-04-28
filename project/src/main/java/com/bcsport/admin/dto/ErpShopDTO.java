package com.bcsport.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * ERP 店铺数据传输对象（新增或修改完
 */
@Data
public class ErpShopDTO {

    private String id;

    @NotBlank(message = "店铺编码不能为空")
    private String shopCode;

    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    @NotBlank(message = "店铺类型不能为空")
    private String shopType;

    private String province;

    private String city;

    private String district;

    private String address;

    private String contactPerson;

    private String contactPhone;

    @NotNull(message = "状态不能为null")
    private Integer status;

    private Integer sort;

    private String remark;
}
