package com.bcsport.admin.dto;

import lombok.Data;

/**
 * ERP 仓库查询数据传输对象
 */
@Data
public class ErpWarehouseQueryDTO {

    private String warehouseCode;

    private String warehouseName;

    private String warehouseType;

    private String province;

    private String city;

    private Integer status;
}
