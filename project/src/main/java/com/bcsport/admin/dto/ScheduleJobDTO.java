package com.bcsport.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@ApiModel(value = "ScheduleJobDTO", description = "定时任务新增/修改")
public class ScheduleJobDTO {

    private String id;

    @NotBlank(message = "任务名称不能为空")
    @ApiModelProperty(value = "任务名称", required = true)
    private String jobName;

    @NotBlank(message = "请选择预设任务")
    @ApiModelProperty(value = "预设任务标识", required = true)
    private String taskKey;

    @NotBlank(message = "Cron表达式不能为空")
    @ApiModelProperty(value = "cron表达式", required = true)
    private String cronExpression;

    @ApiModelProperty("状态：0-暂停，1-运行")
    private Integer status;

    @ApiModelProperty("任务模块(IHR/QW/DEMO/OTHER)")
    private String module;

    @ApiModelProperty("排序号")
    private Integer sort;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("任务参数(JSON)")
    private String params;

    @ApiModelProperty("推送策略(ALWAYS/FAIL_ONLY/DISABLED)")
    private String notifyStrategy;
}
