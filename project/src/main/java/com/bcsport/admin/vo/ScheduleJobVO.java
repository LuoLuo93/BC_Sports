package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "ScheduleJobVO", description = "定时任务配置展示")
public class ScheduleJobVO {

    private String id;

    @ApiModelProperty("任务名称")
    private String jobName;

    @ApiModelProperty("预设任务标识")
    private String taskKey;

    @ApiModelProperty("预设任务描述")
    private String taskDescription;

    @ApiModelProperty("Bean名称")
    private String beanName;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("cron表达式")
    private String cronExpression;

    @ApiModelProperty("状态：0-暂停，1-运行")
    private Integer status;

    @ApiModelProperty("任务模块(IHR/QW/DEMO/OTHER)")
    private String module;

    @ApiModelProperty("排序号")
    private Integer sort;

    @ApiModelProperty("备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("下次执行时间")
    private LocalDateTime nextExecuteTime;
}
