package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value = "IhrEmployeeOnboardingVO", description = "入职员工信息VO")
public class IhrEmployeeOnboardingVO {

    @ApiModelProperty("员工ID(IHR)")
    private String employeesId;

    @ApiModelProperty("员工姓名")
    private String staffName;

    @ApiModelProperty("工号")
    private String staffNo;

    @ApiModelProperty("手机号")
    private String mobileNo;

    @ApiModelProperty("部门")
    private String departmentName;

    @ApiModelProperty("入职日期")
    private String enrollInDate;

    @ApiModelProperty("员工状态")
    private String staffStatus;

    @ApiModelProperty("企微同步状态: null=未同步, 0=未同步, 1=同步成功, 2=同步失败, 3=已跳过")
    private Integer syncStatus;

    @ApiModelProperty("同步时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncTime;

    @ApiModelProperty("同步错误信息")
    private String errorMessage;
}
