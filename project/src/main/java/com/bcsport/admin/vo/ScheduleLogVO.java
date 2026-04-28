package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "ScheduleLogVO", description = "执行日志展示")
public class ScheduleLogVO {

    private String id;

    @ApiModelProperty("关联任务ID")
    private String jobId;

    @ApiModelProperty("任务名称")
    private String jobName;

    @ApiModelProperty("触发类型(CRON/MANUAL)")
    private String triggerType;

    @ApiModelProperty("执行状态：0-失败，1-成功")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime executeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finishTime;

    @ApiModelProperty("执行耗时（毫秒）")
    private Long duration;

    @ApiModelProperty("异常信息")
    private String errorMsg;
}
