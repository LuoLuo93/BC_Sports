package com.bcsport.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

/**
 * 统一的定时任务线程池配置
 */
@Slf4j
@Configuration
public class TaskThreadPoolConfig {

    // 核心线程数 - CPU密集型建议：CPU核心数 + 1
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;

    // 最大线程数
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    // 线程空闲存活时间
    private static final long KEEP_ALIVE_TIME = 60L;

    // 队列容量
    private static final int QUEUE_CAPACITY = 500;

    // 线程池关闭等待时间（秒）
    private static final int SHUTDOWN_WAIT_TIME = 60;

    private ThreadPoolExecutor threadPoolExecutor;

    @Bean("taskThreadPool")
    public ThreadPoolExecutor taskThreadPool() {
        log.info("初始化定时任务线程池，核心线程数：{}，最大线程数：{}", CORE_POOL_SIZE, MAX_POOL_SIZE);

        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = defaultFactory.newThread(r);
                        thread.setName("task-pool-" + (++count));
                        thread.setDaemon(true); // 设置为守护线程，应用关闭时自动结束
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        log.warn("线程池队列已满，由调用者线程执行任务");
                        super.rejectedExecution(r, e);
                    }
                }
        );

        return threadPoolExecutor;
    }

    /**
     * Spring容器关闭时优雅关闭线程池
     */
    @PreDestroy
    public void shutdownThreadPool() {
        log.info("开始关闭定时任务线程池...");

        if (threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
            // 1. 停止接收新任务
            threadPoolExecutor.shutdown();

            try {
                // 2. 等待已提交的任务执行完成
                if (!threadPoolExecutor.awaitTermination(SHUTDOWN_WAIT_TIME, TimeUnit.SECONDS)) {
                    log.warn("线程池在{}秒内未能完全关闭，强制关闭...", SHUTDOWN_WAIT_TIME);
                    // 3. 强制关闭所有正在执行的任务
                    threadPoolExecutor.shutdownNow();

                    // 4. 再次等待一段时间
                    if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        log.error("线程池强制关闭失败！");
                    }
                }

                log.info("定时任务线程池已成功关闭");
            } catch (InterruptedException e) {
                log.error("关闭线程池时被中断", e);
                Thread.currentThread().interrupt();
                threadPoolExecutor.shutdownNow();
            }
        }
    }
}
