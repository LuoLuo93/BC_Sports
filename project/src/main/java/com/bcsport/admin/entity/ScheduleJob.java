package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_schedule_job")
@ApiModel(value = "ScheduleJob", description = "定时任务配置")
public class ScheduleJob extends BaseEntity {

    @ApiModelProperty("任务名称")
    @TableField("job_name")
    private String jobName;

    @ApiModelProperty("预设任务标识")
    @TableField("task_key")
    private String taskKey;

    @ApiModelProperty("cron表达式")
    @TableField("cron_expression")
    private String cronExpression;

    @ApiModelProperty("状态：0-暂停，1-运行")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("任务模块(IHR/QW/DEMO/OTHER)")
    @TableField("module")
    private String module;

    @ApiModelProperty("排序号")
    @TableField("sort")
    private Integer sort;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("任务参数(JSON)")
    @TableField("PARAMS")
    private String params;

    @ApiModelProperty("推送策略(ALWAYS/FAIL_ONLY/DISABLED)")
    @TableField("notify_strategy")
    private String notifyStrategy;
}
