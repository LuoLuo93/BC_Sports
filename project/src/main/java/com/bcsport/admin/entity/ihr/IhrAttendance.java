package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 考勤数据
 */
@Data
@TableName("attendance")
public class IhrAttendance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    private String staffId;
    private String supposedAttendanceDays;
    private String supposedAttendanceHours;
    private String actualAttendanceDays;
    private String actualAttendanceHours;
    private String restDays;
    private String absenceHours;
    private String absenceNumber;
    private String absenceTimes;
    private String appealTimes;
    private String lateMinutes;
    private String lateTimes;
    private String earlyMinutes;
    private String earlyTimes;
    private String signInMissingTimes;
    private String signOutMissingTimes;
    private String businessTravelDays;
    private String fieldWorkHours;
    private String normalHour;
    private String weekendHour;
    private String statutoryHour;
    private String normalToRestHour;
    private String weekendToRestHour;
    private String statutoryToRestHour;
    private String normalToSalaryHour;
    private String weekendToSalaryHour;
    private String statutoryToSalaryHour;
    private String workdayDelayHours;
    private String dayOffDelayHours;
    private String holidayDelayHours;
    private String deepNightDuration;
    private String annualLeave;
    private String personalLeave;
    private String breastfeedingLeave;
    private String compensatedLeave;
    private String mourningLeave;
    private String fullPaySickLeave;
    private String paidSickLeave;
    private String paternityLeave;
    private String homeLeave;
    private String marriageLeave;
    private String maternityLeave;
    private String otherVacation;
    private String prenatalCheckup;
    private String remark;
    private String updateDate;
    private Date createTime;
    private Date updateTime;
}
