package com.bcsport.admin.task.ihr;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.ihr.*;
import com.bcsport.admin.ihrmapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IHR任务：考勤数据同步
 */
@Slf4j
@Component("ihrAttendanceTask")
public class IhrAttendanceTask {

    private static final int ATTENDANCE_PAGE = 0;
    private static final int ATTENDANCE_SIZE = 100;
    private static final int BATCH_SIZE = 10;

    @Autowired
    private IhrApiClient apiClient;

    @Autowired
    private IhrAttendanceSettingsMapper settingsMapper;

    @Autowired
    private IhrAttendanceMapper attendanceMapper;

    @Autowired
    private IhrEmployeesAuxiliaryMapper auxiliaryMapper;

    @Autowired
    @Qualifier("ihrTransactionManager")
    private PlatformTransactionManager transactionManager;

    /**
     * 同步考勤周期设置
     */
    public void syncSettings() {
        log.info("=== 开始执行: IHR同步考勤设置 ===");
        try {
            // API调用在事务外执行
            JSONArray data = apiClient.getDataArray(
                    "/openapi/thirdparty/api/tm/v1/attendance/periods/settings/search");

            if (data == null || data.isEmpty()) {
                log.warn("=== 完成: IHR同步考勤设置, 无数据 ===");
                return;
            }

            // 数据准备在事务外完成
            List<IhrAttendanceSettings> list = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JSONObject obj = data.getJSONObject(i);
                IhrAttendanceSettings s = new IhrAttendanceSettings();
                s.setId(obj.getStr("id"));
                s.setPeriodName(obj.getStr("periodName"));
                s.setPeriodStartMonth(obj.getStr("periodStartMonth"));
                s.setPeriodStartDay(obj.getStr("periodStartDay"));
                s.setPeriodEndMonth(obj.getStr("periodEndMonth"));
                s.setPeriodEndDay(obj.getStr("periodEndDay"));
                s.setDefaultPeriod(obj.getStr("defaultPeriod"));
                s.setEnabled(obj.getStr("enabled"));
                list.add(s);
            }

            // 短事务仅用于DB写入
            doSyncSettings(list);

            log.info("=== 完成: IHR同步考勤设置, 共{}条 ===", list.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步考勤设置: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSyncSettings(List<IhrAttendanceSettings> list) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            settingsMapper.deleteAll();
            for (int i = 0; i < list.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, list.size());
                settingsMapper.insertBatch(list.subList(i, end));
            }
            return null;
        });
    }

    /**
     * 同步月度考勤数据
     */
    public void syncData() {
        log.info("=== 开始执行: IHR同步考勤数据 ===");
        try {
            // DB读取（只读操作，无需长事务）
            List<IhrEmployeesAuxiliary> auxiliaries = auxiliaryMapper.selectList(null);
            if (auxiliaries.isEmpty()) {
                log.warn("=== 完成: IHR同步考勤数据, 无员工ID ===");
                return;
            }

            // 获取考勤周期设置ID
            List<IhrAttendanceSettings> settings = settingsMapper.selectList(null);
            if (settings.isEmpty()) {
                log.warn("=== 完成: IHR同步考勤数据, 无考勤设置 ===");
                return;
            }
            String periodSettingId = settings.get(0).getId();

            // 当前月份
            String currentMonth = LocalDate.now().toString().substring(0, 7);

            // API调用在事务外执行
            List<IhrAttendance> allAttendanceList = new ArrayList<>();
            for (IhrEmployeesAuxiliary aux : auxiliaries) {
                try {
                    allAttendanceList.addAll(fetchAttendanceDataList(aux.getEmployeesId(), periodSettingId, currentMonth));
                } catch (Exception e) {
                    log.warn("获取考勤数据失败, staffId: {}, 错误: {}", aux.getEmployeesId(), e.getMessage());
                }
            }

            // 短事务仅用于DB写入
            doSyncData(allAttendanceList);

            log.info("=== 完成: IHR同步考勤数据, 共{}条 ===", allAttendanceList.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步考勤数据: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSyncData(List<IhrAttendance> allAttendanceList) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            attendanceMapper.deleteAll();
            for (int i = 0; i < allAttendanceList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allAttendanceList.size());
                attendanceMapper.insertBatch(allAttendanceList.subList(i, end));
            }
            return null;
        });
    }

    private List<IhrAttendance> fetchAttendanceDataList(String staffId, String periodSettingId, String periodMonth) {
        List<IhrAttendance> result = new ArrayList<>();
        JSONObject bodyJson = cn.hutool.json.JSONUtil.createObj()
                .set("staffId", staffId)
                .set("periodSettingId", periodSettingId)
                .set("periodMonth", periodMonth)
                .set("page", ATTENDANCE_PAGE)
                .set("size", ATTENDANCE_SIZE);
        String body = bodyJson.toString();

        JSONObject response = apiClient.postJsonObject(
                "/openapi/thirdparty/api/tm/v2/period/monthly/reports/search", body);
        JSONObject data = response.getJSONObject("data");
        if (data == null) return result;

        JSONArray content = data.getJSONArray("content");
        if (content == null || content.isEmpty()) return result;

        for (int i = 0; i < content.size(); i++) {
            JSONObject obj = content.getJSONObject(i);
            IhrAttendance a = new IhrAttendance();
            a.setId(obj.getStr("id"));
            a.setStaffId(obj.getStr("staffId"));
            a.setSupposedAttendanceDays(obj.getStr("supposedAttendanceDays"));
            a.setSupposedAttendanceHours(obj.getStr("supposedAttendanceHours"));
            a.setActualAttendanceDays(obj.getStr("actualAttendanceDays"));
            a.setActualAttendanceHours(obj.getStr("actualAttendanceHours"));
            a.setRestDays(obj.getStr("restDays"));
            a.setAbsenceHours(obj.getStr("absenceHours"));
            a.setAbsenceNumber(obj.getStr("absenceNumber"));
            a.setAbsenceTimes(obj.getStr("absenceTimes"));
            a.setAppealTimes(obj.getStr("appealTimes"));
            a.setLateMinutes(obj.getStr("lateMinutes"));
            a.setLateTimes(obj.getStr("lateTimes"));
            a.setEarlyMinutes(obj.getStr("earlyMinutes"));
            a.setEarlyTimes(obj.getStr("earlyTimes"));
            a.setSignInMissingTimes(obj.getStr("signInMissingTimes"));
            a.setSignOutMissingTimes(obj.getStr("signOutMissingTimes"));
            a.setBusinessTravelDays(obj.getStr("businessTravelDays"));
            a.setFieldWorkHours(obj.getStr("fieldWorkHours"));
            a.setNormalHour(obj.getStr("normalHour"));
            a.setWeekendHour(obj.getStr("weekendHour"));
            a.setStatutoryHour(obj.getStr("statutoryHour"));
            a.setNormalToRestHour(obj.getStr("normalToRestHour"));
            a.setWeekendToRestHour(obj.getStr("weekendToRestHour"));
            a.setStatutoryToRestHour(obj.getStr("statutoryToRestHour"));
            a.setNormalToSalaryHour(obj.getStr("normalToSalaryHour"));
            a.setWeekendToSalaryHour(obj.getStr("weekendToSalaryHour"));
            a.setStatutoryToSalaryHour(obj.getStr("statutoryToSalaryHour"));
            a.setWorkdayDelayHours(obj.getStr("workdayDelayHours"));
            a.setDayOffDelayHours(obj.getStr("dayOffDelayHours"));
            a.setHolidayDelayHours(obj.getStr("holidayDelayHours"));
            a.setDeepNightDuration(obj.getStr("deepNightDuration"));
            a.setAnnualLeave(obj.getStr("annualLeave"));
            a.setPersonalLeave(obj.getStr("personalLeave"));
            a.setBreastfeedingLeave(obj.getStr("breastfeedingLeave"));
            a.setCompensatedLeave(obj.getStr("compensatoryLeave"));
            a.setMourningLeave(obj.getStr("mourningLeave"));
            a.setFullPaySickLeave(obj.getStr("fullPaySickLeave"));
            a.setPaidSickLeave(obj.getStr("paidSickLeave"));
            a.setPaternityLeave(obj.getStr("paternityLeave"));
            a.setHomeLeave(obj.getStr("homeLeave"));
            a.setMarriageLeave(obj.getStr("marriageLeave"));
            a.setMaternityLeave(obj.getStr("maternityLeave"));
            a.setOtherVacation(obj.getStr("otherVacation"));
            a.setPrenatalCheckup(obj.getStr("prenatalCheckUp"));
            a.setRemark(obj.getStr("remark"));
            a.setUpdateDate(obj.getStr("updateDate"));
            result.add(a);
        }
        return result;
    }
}
