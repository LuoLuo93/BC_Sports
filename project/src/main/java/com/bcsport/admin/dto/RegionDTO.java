package com.bcsport.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 地区数据传输对象（新增或修改完 */
@Data
public class RegionDTO {

    private String id;
    
    private String parentId;
    
    @NotBlank(message = "地区名称不能为空")
    private String regionName;
    
    private String regionCode;
    
    @NotNull(message = "地区类型不能为空")
    private Integer regionType;
    
    private Integer sort;
    
    @NotNull(message = "状态不能为null")
    private Integer status;
    
    private String remark;
}
