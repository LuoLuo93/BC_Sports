package com.bcsport.admin.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 品牌数据传输对象（新增或修改完 */
@Data
public class BrandDTO {
    
    private String id;
    
    @NotBlank(message = "品牌名称不能为空")
    private String brandName;
    
    private String brandLogo;
    
    private String description;
    
    @NotNull(message = "状态不能为null")
    private Integer status;
    
    private Integer sort;
}
