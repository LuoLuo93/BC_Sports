package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 品牌查询传输对象
 */
@Data
public class BrandQueryDTO {
    
    private String brandName;
    
    private Integer status;
}
