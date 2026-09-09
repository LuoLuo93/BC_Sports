package com.bcsport.admin.config;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.entity.ScheduleJob;
import com.bcsport.admin.entity.ScheduleLog;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.ScheduleLogService;
import com.bcsport.admin.service.notify.NotifyManager;
import com.bcsport.admin.task.ScheduleTaskRegistry;
import com.bcsport.admin.util.CronUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import cn.hutool.json.JSONUtil;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    private NotifyManager notifyManager;

    @Autowired
    private ConfigService configService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    // F43: 定时任务分布式锁(Redis SETNX + TTL,实例崩溃后 TTL 自动过期)。
    // TTL 取 2 小时,长于最长的同步任务(订单同步 60 分钟);Redis 不可用时回退 JVM 内锁。
    private static final String REDIS_LOCK_PREFIX = "schedule:lock:";
    private static final Duration REDIS_LOCK_TTL = Duration.ofHours(2);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private ThreadPoolTaskScheduler taskScheduler;

    /**
     * 手动执行专用池：与 cron 调度分离，避免多个手动触发的长任务(IHR/QW全量同步动辄数十分钟)
     * 占满调度线程导致所有 cron 触发被排队延迟；也避免调度器关闭中 execute 直接拒绝。
     */
    private ThreadPoolExecutor manualExecutor;

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("schedule-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(60);
        taskScheduler.initialize();

        manualExecutor = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("schedule-manual-" + t.getId());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
        log.info("定时任务调度器初始化完成, cron池：10, 手动执行池：2-4");
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

        try {
            manualExecutor.execute(createRunnable(job, option, "MANUAL"));
        } catch (RejectedExecutionException e) {
            // 先加的执行标记必须回收，否则该任务在重启前永远显示"执行中"
            runningJobStartTimes.remove(job.getId());
            runningJobNames.remove(job.getId());
            runningJobIds.remove(job.getId());
            log.error("手动执行池已满或关闭, 任务[{}]提交失败", job.getJobName());
            throw new IllegalStateException("手动执行队列已满，请稍后再试");
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduledFutures.values().forEach(future -> future.cancel(false));
        scheduledFutures.clear();
        runningLocks.clear();
        if (manualExecutor != null) {
            manualExecutor.shutdown();
            try {
                if (!manualExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    manualExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                manualExecutor.shutdownNow();
            }
        }
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
            long startMillis = System.currentTimeMillis();
            LocalDateTime startTime = LocalDateTime.now();
            ScheduleLog scheduleLog = new ScheduleLog();
            scheduleLog.setId(IdWorker.getIdStr());
            scheduleLog.setJobId(job.getId());
            scheduleLog.setJobName(job.getJobName());
            scheduleLog.setTriggerType(triggerType);
            scheduleLog.setExecuteTime(startTime);
            scheduleLog.setCreateBy("system");

            String lockKey = getLockKey(option);
            String lockToken = UUID.randomUUID().toString();
            ReentrantLock runningLock = null;
            boolean locked = false;      // 已持有锁(Redis 或 JVM)
            boolean jvmLocked = false;   // 锁类型为 JVM 锁（finally 区分解锁方式）
            boolean skipped = false;  // 标记是否被跳过
            try {
                boolean redisLockError = false;
                try {
                    locked = stringRedisTemplate != null && Boolean.TRUE.equals(
                            stringRedisTemplate.opsForValue()
                                    .setIfAbsent(REDIS_LOCK_PREFIX + lockKey, lockToken, REDIS_LOCK_TTL));
                } catch (Exception e) {
                    redisLockError = true;
                    log.warn("Redis 分布式锁不可用，回退 JVM 内锁: lockKey={}, error={}", lockKey, e.getMessage());
                }
                if (!locked) {
                    if (!redisLockError) {
                        // 其他实例正在执行同模块任务
                        skipped = true;
                        scheduleLog.setStatus(0);
                        scheduleLog.setErrorMsg("Task skipped because module is already running: " + lockKey);
                        log.warn("定时任务跳过: [{}] {}, lockKey={}", job.getId(), job.getJobName(), lockKey);
                        return;
                    }
                    runningLock = runningLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
                    locked = jvmLocked = runningLock.tryLock();
                    if (!locked) {
                        skipped = true;
                        scheduleLog.setStatus(0);
                        scheduleLog.setErrorMsg("Task skipped because module is already running: " + lockKey);
                        log.warn("定时任务跳过: [{}] {}, lockKey={}", job.getId(), job.getJobName(), lockKey);
                        return;
                    }
                }

                Object bean = applicationContext.getBean(option.getBeanName());
                boolean hasParams = job.getParams() != null && !job.getParams().trim().isEmpty();
                Method method = findMethod(bean.getClass(), option.getMethodName(), hasParams);
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
                LocalDateTime endTime = LocalDateTime.now();
                scheduleLog.setFinishTime(endTime);
                scheduleLog.setDuration(System.currentTimeMillis() - startMillis);
                try {
                    logService.saveLog(scheduleLog);
                } finally {
                    if (locked) {
                        if (jvmLocked) {
                            runningLock.unlock();
                        } else {
                            try {
                                stringRedisTemplate.execute(UNLOCK_SCRIPT,
                                        Collections.singletonList(REDIS_LOCK_PREFIX + lockKey), lockToken);
                            } catch (Exception e) {
                                log.warn("Redis 分布式锁释放失败(TTL 到期自动过期): lockKey={}, error={}",
                                        lockKey, e.getMessage());
                            }
                        }
                    }
                    if ("MANUAL".equals(triggerType)) {
                        runningJobIds.remove(job.getId());
                        runningJobNames.remove(job.getId());
                        runningJobStartTimes.remove(job.getId());
                    }
                }

                // 根据推送策略发送通知（跳过的任务不推送）
                if (!skipped) {
                    try {
                        String notifyStrategy = job.getNotifyStrategy();
                        boolean shouldNotify = shouldNotify(notifyStrategy, scheduleLog.getStatus());
                        if (shouldNotify) {
                            notifyManager.sendTaskResult(
                                    job.getJobName(),
                                    scheduleLog.getStatus(),
                                    triggerType,
                                    startTime,
                                    endTime,
                                    scheduleLog.getDuration(),
                                    scheduleLog.getErrorMsg()
                            );
                        }
                    } catch (Exception e) {
                        log.warn("发送通知失败: {}", e.getMessage());
                    }
                }
            }
        };
    }

    private String getLockKey(ScheduleTaskRegistry.TaskOption option) {
        String module = option.getModule();
        if (module != null && !module.trim().isEmpty()) {
            // TICKET 模块不做模块级唯一校验，使用任务级锁允许并发执行
            if (ScheduleTaskRegistry.MODULE_TICKET.equals(module)) {
                return "task:" + option.getTaskKey();
            }
            return "module:" + module;
        }
        return "task:" + option.getTaskKey();
    }

    private Method findMethod(Class<?> clazz, String methodName, boolean preferMapParam) throws NoSuchMethodException {
        Method fallback = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                Class<?>[] pts = m.getParameterTypes();
                boolean isMapVersion = pts.length == 1 && Map.class.isAssignableFrom(pts[0]);
                if (preferMapParam && isMapVersion) return m;
                if (!preferMapParam && pts.length == 0) return m;
                if (fallback == null) fallback = m;
            }
        }
        if (fallback != null) return fallback;
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return findMethod(superClass, methodName, preferMapParam);
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

    /**
     * 判断是否需要发送通知
     * @param notifyStrategy 推送策略 (ALWAYS/FAIL_ONLY/DISABLED/null)
     * @param status 执行状态 (1=成功, 0=失败)
     * @return true=需要推送
     */
    private boolean shouldNotify(String notifyStrategy, Integer status) {
        // 未配置策略时，使用全局默认策略
        if (notifyStrategy == null || notifyStrategy.isBlank()) {
            notifyStrategy = configService.getString("schedule.notify.defaultStrategy", "DISABLED");
        }

        switch (notifyStrategy) {
            case "ALWAYS":
                return true;
            case "FAIL_ONLY":
                return status != null && status == 0;
            case "DISABLED":
                return false;
            default:
                return false;
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
