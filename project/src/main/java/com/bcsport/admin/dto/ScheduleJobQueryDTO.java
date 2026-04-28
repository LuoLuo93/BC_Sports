package com.bcsport.admin.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ScheduleJobQueryDTO {

    @ApiModelProperty("任务名称")
    private String jobName;

    @ApiModelProperty("状态：0-暂停，1-运行")
    private Integer status;

    @ApiModelProperty("任务模块(IHR/QW/DEMO/OTHER)")
    private String module;
}
