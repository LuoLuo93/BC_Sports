package com.bcsport.admin.config;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.entity.ScheduleJob;
import com.bcsport.admin.entity.ScheduleLog;
import com.bcsport.admin.service.ScheduleLogService;
import com.bcsport.admin.task.ScheduleTaskRegistry;
import com.bcsport.admin.util.CronUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@EnableScheduling
public class ScheduleConfig {

    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduleLogService logService;

    private ThreadPoolTaskScheduler taskScheduler;

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("schedule-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(60);
        taskScheduler.initialize();
        log.info("定时任务调度器初始化完成, 线程池大小：10");
    }

    /**
     * 将5位Linux格式cron转换为6位Spring格式（自动补秒位为0）
     */
    private String normalizeCron(String cronExpression) {
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

    public void registerTask(ScheduleJob job) {
        removeTask(job.getId());

        ScheduleTaskRegistry.TaskOption option = ScheduleTaskRegistry.getTask(job.getTaskKey());
        if (option == null) {
            log.warn("预设任务不存在 {}", job.getTaskKey());
            return;
        }

        Runnable runnable = createRunnable(job, option, "CRON");
        String normalizedCron = normalizeCron(job.getCronExpression());
        CronTrigger cronTrigger = new CronTrigger(normalizedCron);
        ScheduledFuture<?> future = taskScheduler.schedule(runnable, cronTrigger);
        scheduledFutures.put(job.getId(), future);
        log.info("注册定时任务: [{}] {}, cron: {}", job.getId(), job.getJobName(), normalizedCron);
    }

    public void removeTask(String jobId) {
        ScheduledFuture<?> future = scheduledFutures.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("移除定时任务: [{}]", jobId);
        }
    }

    public void executeJobImmediately(ScheduleJob job) {
        ScheduleTaskRegistry.TaskOption option = ScheduleTaskRegistry.getTask(job.getTaskKey());
        if (option == null) {
            log.warn("预设任务不存在 {}", job.getTaskKey());
            return;
        }
        taskScheduler.execute(createRunnable(job, option, "MANUAL"));
    }

    private Runnable createRunnable(ScheduleJob job, ScheduleTaskRegistry.TaskOption option, String triggerType) {
        return () -> {
            long startTime = System.currentTimeMillis();
            ScheduleLog scheduleLog = new ScheduleLog();
            scheduleLog.setId(IdWorker.getIdStr());
            scheduleLog.setJobId(job.getId());
            scheduleLog.setJobName(job.getJobName());
            scheduleLog.setTriggerType(triggerType);
            scheduleLog.setExecuteTime(LocalDateTime.now());
            scheduleLog.setCreateBy("system");

            try {
                Object bean = applicationContext.getBean(option.getBeanName());
                Method method = findMethod(bean.getClass(), option.getMethodName());
                method.setAccessible(true);
                method.invoke(bean);
                scheduleLog.setStatus(1);
            } catch (Exception e) {
                scheduleLog.setStatus(0);
                String errorMsg = getExceptionMessage(e);
                scheduleLog.setErrorMsg(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
                log.error("定时任务执行失败: [{}] {}", job.getJobName(), e.getMessage(), e);
            } finally {
                scheduleLog.setFinishTime(LocalDateTime.now());
                scheduleLog.setDuration(System.currentTimeMillis() - startTime);
                logService.saveLog(scheduleLog);
            }
        };
    }

    private Method findMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return findMethod(superClass, methodName);
        }
        throw new NoSuchMethodException("方法不存在：" + methodName);
    }

    private String getExceptionMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable cause = e;
        while (cause != null && sb.length() < 2000) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return sb.toString();
    }
}
