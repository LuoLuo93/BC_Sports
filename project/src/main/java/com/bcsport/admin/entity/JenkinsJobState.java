package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Jenkins 构建监控状态表（每个 Jenkins 任务一行）
 * 记录上次轮询看到的最近完成构建号与结果，用于增量比对防止重复告警
 */
@Data
@TableName("bc_sports_jenkins_job_state")
public class JenkinsJobState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Jenkins任务名(主键) */
    @TableId(type = IdType.INPUT)
    private String jobName;

    /** 上次看到的完成构建号 */
    private Long lastBuildNumber;

    /** 上次看到的构建结果(SUCCESS/FAILURE/UNSTABLE/ABORTED/NOT_BUILT) */
    private String lastResult;

    /** 上次更新时间 */
    private Date updateTime;
}
