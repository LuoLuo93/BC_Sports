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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import cn.hutool.json.JSONUtil;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@EnableScheduling
public class ScheduleConfig {

    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> runningLocks = new ConcurrentHashMap<>();

    // 手动执行任务状态跟踪（按 jobId 管理，防同任务重复触发，不阻塞其他任务）
    private static final Set<String> runningJobIds = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> runningJobNames = new ConcurrentHashMap<>();
    private static final Map<String, Long> runningJobStartTimes = new ConcurrentHashMap<>();

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

        // 防止同一任务重复触发（不同任务互不影响）
        if (!runningJobIds.add(job.getId())) {
            log.warn("任务[{}]正在执行中，拒绝重复触发", job.getJobName());
            throw new IllegalStateException("任务「" + job.getJobName() + "」正在执行中，请等待完成后再试");
        }

        runningJobNames.put(job.getId(), job.getJobName());
        runningJobStartTimes.put(job.getId(), System.currentTimeMillis());

        taskScheduler.execute(createRunnable(job, option, "MANUAL"));
    }

    @PreDestroy
    public void shutdown() {
        scheduledFutures.values().forEach(future -> future.cancel(false));
        scheduledFutures.clear();
        runningLocks.clear();
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }

    /**
     * 判断指定任务是否正在手动执行
     */
    public static boolean isJobRunning(String jobId) {
        return runningJobIds.contains(jobId);
    }

    /**
     * 是否有任意手动任务正在执行
     */
    public static boolean isAnyManualRunning() {
        return !runningJobIds.isEmpty();
    }

    /**
     * 获取当前执行中任务的名称（第一个）
     */
    public static String getFirstRunningJobName() {
        return runningJobNames.isEmpty() ? null : runningJobNames.values().iterator().next();
    }

    /**
     * 获取指定任务已运行秒数
     */
    public static long getJobElapsedSeconds(String jobId) {
        Long startTime = runningJobStartTimes.get(jobId);
        if (startTime == null) return 0;
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * 获取所有正在执行的任务ID集合
     */
    public static Set<String> getRunningJobIds() {
        return new HashSet<>(runningJobIds);
    }

    /**
     * 获取任务名称
     */
    public static String getJobName(String jobId) {
        return runningJobNames.get(jobId);
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

            String lockKey = getLockKey(option);
            ReentrantLock runningLock = runningLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
            boolean locked = false;
            try {
                locked = runningLock.tryLock();
                if (!locked) {
                    scheduleLog.setStatus(0);
                    scheduleLog.setErrorMsg("Task skipped because module is already running: " + lockKey);
                    log.warn("定时任务跳过: [{}] {}, lockKey={}", job.getId(), job.getJobName(), lockKey);
                    return;
                }

                Object bean = applicationContext.getBean(option.getBeanName());
                Method method = findMethod(bean.getClass(), option.getMethodName());
                method.setAccessible(true);
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1 && Map.class.isAssignableFrom(paramTypes[0])) {
                    Map<String, String> paramMap = parseParams(job.getParams());
                    method.invoke(bean, paramMap);
                } else {
                    method.invoke(bean);
                }
                scheduleLog.setStatus(1);
            } catch (Exception e) {
                scheduleLog.setStatus(0);
                String errorMsg = getExceptionMessage(e);
                scheduleLog.setErrorMsg(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
                log.error("定时任务执行失败: [{}] {}", job.getJobName(), cause != null ? cause.getMessage() : e.getMessage(), e);
            } finally {
                scheduleLog.setFinishTime(LocalDateTime.now());
                scheduleLog.setDuration(System.currentTimeMillis() - startTime);
                try {
                    logService.saveLog(scheduleLog);
                } finally {
                    if (locked) {
                        runningLock.unlock();
                    }
                    if ("MANUAL".equals(triggerType)) {
                        runningJobIds.remove(job.getId());
                        runningJobNames.remove(job.getId());
                        runningJobStartTimes.remove(job.getId());
                    }
                }
            }
        };
    }

    private String getLockKey(ScheduleTaskRegistry.TaskOption option) {
        String module = option.getModule();
        if (module != null && !module.trim().isEmpty()) {
            return "module:" + module;
        }
        return "task:" + option.getTaskKey();
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

    private Map<String, String> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.trim().isEmpty()) return null;
        try {
            cn.hutool.json.JSONObject obj = JSONUtil.parseObj(paramsJson);
            Map<String, String> map = new java.util.HashMap<>();
            for (String key : obj.keySet()) {
                Object val = obj.get(key);
                map.put(key, val != null ? val.toString() : null);
            }
            return map;
        } catch (Exception e) {
            log.warn("解析任务参数失败: {}", paramsJson, e);
            return null;
        }
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
