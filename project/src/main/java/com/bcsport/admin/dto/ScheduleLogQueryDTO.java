package com.bcsport.admin.dto;

import lombok.Data;

@Data
public class ScheduleLogQueryDTO {
    private String jobId;
    private String jobName;
    private String triggerType;
    private Integer status;
}
