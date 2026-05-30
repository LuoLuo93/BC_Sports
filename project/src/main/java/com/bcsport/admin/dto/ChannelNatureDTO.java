package com.bcsport.admin.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 渠道性质数据传输对象（新增或修改完
 */
@Data
public class ChannelNatureDTO {

    private String id;

    private String parentId;

    @NotBlank(message = "性质名称不能为空")
    private String natureName;

    private String natureCode;

    private Integer sort;

    @NotNull(message = "状态不能为null")
    private Integer status;

    private String remark;
}
