package com.bcsport.admin.util;

import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CronUtils {

    /**
     * 将5位Linux格式cron转换为6位Spring格式（自动补秒位为0）
     */
    private static String normalizeCron(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return cronExpression;
        }
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length == 5) {
            // 5位格式: 分 时 日 月 周 -> 补秒位为0
            return "0 " + cronExpression.trim();
        }
        return cronExpression;
    }

    public static boolean isValid(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        try {
            String normalized = normalizeCron(cronExpression);
            new CronTrigger(normalized);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static LocalDateTime getNextExecution(String cronExpression) {
        try {
            String normalized = normalizeCron(cronExpression);
            CronTrigger trigger = new CronTrigger(normalized);
            Date next = trigger.nextExecutionTime(new SimpleTriggerContext());
            return next == null ? null :
                    LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    public static List<LocalDateTime> getNextExecutions(String cronExpression, int count) {
        List<LocalDateTime> result = new ArrayList<>();
        try {
            String normalized = normalizeCron(cronExpression);
            CronTrigger trigger = new CronTrigger(normalized);
            SimpleTriggerContext context = new SimpleTriggerContext();
            for (int i = 0; i < count; i++) {
                Date next = trigger.nextExecutionTime(context);
                if (next == null) break;
                result.add(LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault()));
                context.update(null, next, next);
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
