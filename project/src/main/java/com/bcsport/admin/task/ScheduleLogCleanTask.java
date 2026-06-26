package com.bcsport.admin.task;

import com.bcsport.admin.service.ScheduleLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时清理执行日志：删除7天前的日志记录
 */
@Slf4j
@Component("scheduleLogCleanTask")
public class ScheduleLogCleanTask {

    private static final int KEEP_DAYS = 7;

    @Autowired
    private ScheduleLogService scheduleLogService;

    public void clean() {
        log.info("=== 开始清理{}天前的执行日志 ===", KEEP_DAYS);
        try {
            scheduleLogService.cleanOldLogs(KEEP_DAYS);
            log.info("=== 完成: 清理执行日志 ===");
        } catch (Exception e) {
            log.error("=== 失败: 清理执行日志: {} ===", e.getMessage(), e);
            throw e;
        }
    }
}
