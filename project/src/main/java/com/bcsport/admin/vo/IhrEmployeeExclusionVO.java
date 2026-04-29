
package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "IhrEmployeeExclusionVO", description = "IHR员工排除VO")
public class IhrEmployeeExclusionVO {

    @ApiModelProperty("主键ID")
    private String id;

    @ApiModelProperty("员工姓名")
    private String staffName;

    @ApiModelProperty("工号")
    private String staffNo;

    @ApiModelProperty("排除类型：1-入职排除，2-离职排除")
    private Integer exclusionType;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("排除原因")
    private String reason;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("创建人")
    private String createBy;

    @ApiModelProperty("更新人")
    private String updateBy;
}
