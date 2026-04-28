package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 考勤周期设置
 */
@Data
@TableName("attendance_settings")
public class IhrAttendanceSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    private String periodName;
    private String periodStartMonth;
    private String periodStartDay;
    private String periodEndMonth;
    private String periodEndDay;
    private String defaultPeriod;
    private String enabled;
    private Date createTime;
    private Date updateTime;
}
