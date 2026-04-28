package com.bcsport.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * ERP 仓库数据传输对象（新增或修改完
 */
@Data
public class ErpWarehouseDTO {

    private String id;

    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;

    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    @NotBlank(message = "仓库类型不能为空")
    private String warehouseType;

    private String province;

    private String city;

    private String district;

    private String address;

    private String manager;

    private String contactPhone;

    private Integer capacity;

    private Integer usedCapacity;

    @NotNull(message = "状态不能为null")
    private Integer status;

    private Integer sort;

    private String remark;
}
