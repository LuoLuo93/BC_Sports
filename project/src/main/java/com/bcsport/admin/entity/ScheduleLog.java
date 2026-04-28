package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("bc_sports_sys_schedule_log")
@ApiModel(value = "ScheduleLog", description = "定时任务执行日志")
public class ScheduleLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty("关联任务ID")
    @TableField("job_id")
    private String jobId;

    @ApiModelProperty("任务名称")
    @TableField("job_name")
    private String jobName;

    @ApiModelProperty("触发类型(CRON/MANUAL)")
    @TableField("trigger_type")
    private String triggerType;

    @ApiModelProperty("执行状态：0-失败，1-成功")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("执行时间")
    @TableField("execute_time")
    private LocalDateTime executeTime;

    @ApiModelProperty("完成时间")
    @TableField("finish_time")
    private LocalDateTime finishTime;

    @ApiModelProperty("执行耗时（毫秒）")
    @TableField("duration")
    private Long duration;

    @ApiModelProperty("异常信息")
    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;
}
