package com.bcsport.admin.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 渠道类型数据传输对象（新增或修改完
 */
@Data
public class ChannelTypeDTO {

    private String id;

    private String parentId;

    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    private String typeCode;

    private Integer sort;

    @NotNull(message = "状态不能为null")
    private Integer status;

    private String remark;
}
