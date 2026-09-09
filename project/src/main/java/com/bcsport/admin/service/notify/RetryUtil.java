package com.bcsport.admin.service.notify;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 重试工具类
 *
 * 提供通用的重试机制，支持指数退避
 */
@Slf4j
public class RetryUtil {

    /** 默认最大重试次数 */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /** 默认基础重试间隔（毫秒） */
    private static final long DEFAULT_BASE_DELAY = 1000;

    /** F62: 严格匹配 HTTP 5xx 状态码（替代 contains("HTTP")&&contains("5")——那会把 "HTTP2"、消息里任意"5"误判为可重试） */
    private static final Pattern HTTP_5XX = Pattern.compile("HTTP/?\\s*5\\d{2}\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 执行带重试的操作
     *
     * @param action    要执行的操作
     * @param <T>       返回值类型
     * @return 操作结果
     * @throws Exception 所有重试都失败后抛出最后一次异常
     */
    public static <T> T executeWithRetry(RetryableAction<T> action) throws Exception {
        return executeWithRetry(action, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY);
    }

    /**
     * 执行带重试的操作
     *
     * @param action     要执行的操作
     * @param maxRetries 最大重试次数
     * @param baseDelay  基础重试间隔（毫秒）
     * @param <T>        返回值类型
     * @return 操作结果
     * @throws Exception 所有重试都失败后抛出最后一次异常
     */
    public static <T> T executeWithRetry(RetryableAction<T> action, int maxRetries, long baseDelay) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return action.execute();
            } catch (Exception e) {
                lastException = e;

                // 判断是否可重试
                if (!isRetryable(e) || attempt >= maxRetries) {
                    throw e;
                }

                // 计算退避时间（指数退避）
                long delay = calculateDelay(attempt, baseDelay);
                log.warn("操作失败，第 {}/{} 次重试，等待 {}ms，原因: {}",
                        attempt + 1, maxRetries, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        // 理论上不会执行到这里
        throw lastException;
    }

    /**
     * 执行带重试的操作（无返回值）
     *
     * @param action 要执行的操作
     * @throws Exception 所有重试都失败后抛出最后一次异常
     */
    public static void executeWithRetryVoid(RetryableVoidAction action) throws Exception {
        executeWithRetryVoid(action, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY);
    }

    /**
     * 执行带重试的操作（无返回值）
     *
     * @param action     要执行的操作
     * @param maxRetries 最大重试次数
     * @param baseDelay  基础重试间隔（毫秒）
     * @throws Exception 所有重试都失败后抛出最后一次异常
     */
    public static void executeWithRetryVoid(RetryableVoidAction action, int maxRetries, long baseDelay) throws Exception {
        executeWithRetry(() -> {
            action.execute();
            return null;
        }, maxRetries, baseDelay);
    }

    /**
     * 判断异常是否可重试
     *
     * F62: 以异常类型为准（遍历 cause 链，覆盖被 SDK 包装的情况），
     * 文本匹配仅保留严格 HTTP 5xx 状态码——原先 contains("连接")/("超时") 依赖消息措辞，
     * contains("HTTP")&&contains("5") 会把 "HTTP2"、任意含"5"的消息误判为可重试。
     *
     * 可重试的异常类型：
     * - 网络连接异常 / 超时异常 / DNS 解析失败 / IO 异常
     * - HTTP 5xx 错误（服务器临时错误）
     */
    private static boolean isRetryable(Exception e) {
        // 类型判断：网络/超时类异常（含 cause 链中被包装的情况）
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.ConnectException
                    || t instanceof java.net.SocketTimeoutException
                    || t instanceof java.net.UnknownHostException
                    || t instanceof java.io.IOException) {
                return true;
            }
        }

        // HTTP 5xx（严格状态码匹配）
        String message = e.getMessage();
        return message != null && HTTP_5XX.matcher(message).find();
    }

    /**
     * 计算退避时间（指数退避 + 随机抖动）
     *
     * @param attempt   当前重试次数
     * @param baseDelay 基础延迟
     * @return 退避时间（毫秒）
     */
    private static long calculateDelay(int attempt, long baseDelay) {
        // 指数退避：baseDelay * 2^attempt
        long exponentialDelay = baseDelay * (1L << attempt);

        // 添加随机抖动（±25%）
        double jitter = 0.75 + Math.random() * 0.5;
        long delay = (long) (exponentialDelay * jitter);

        // 最大延迟 30 秒
        return Math.min(delay, 30000);
    }

    /**
     * 可重试的操作（有返回值）
     */
    @FunctionalInterface
    public interface RetryableAction<T> {
        T execute() throws Exception;
    }

    /**
     * 可重试的操作（无返回值）
     */
    @FunctionalInterface
    public interface RetryableVoidAction {
        void execute() throws Exception;
    }
}
