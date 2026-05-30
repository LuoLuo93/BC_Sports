
package com.bcsport.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@ApiModel(value = "IhrEmployeeExclusionDTO", description = "IHR员工排除DTO")
public class IhrEmployeeExclusionDTO {

    @ApiModelProperty("主键ID")
    private String id;

    @NotBlank(message = "员工姓名不能为空")
    @ApiModelProperty("员工姓名")
    private String staffName;

    @NotBlank(message = "工号不能为空")
    @ApiModelProperty("工号")
    private String staffNo;

    @NotNull(message = "排除类型不能为空")
    @Min(value = 1, message = "排除类型：1-入职排除，2-离职排除")
    @Max(value = 2, message = "排除类型：1-入职排除，2-离职排除")
    @ApiModelProperty("排除类型：1-入职排除，2-离职排除")
    private Integer exclusionType;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("排除原因")
    private String reason;
}
