package com.bcsport.admin.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 仓库数据传输对象（新增或修改完
 */
@Data
public class WarehouseDTO {
    
    /**
     * 仓库ID（修改时必填）
     */
    private String id;
    
    /**
     * 仓库名称
     */
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;
    
    /**
     * 仓库编码
     */
    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态（0-禁用 1-启用）
     */
    @NotNull(message = "状态不能为null")
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sort;
}
