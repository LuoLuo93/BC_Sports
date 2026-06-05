package com.bcsport.admin.service.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推送管理器
 *
 * 统一管理所有推送渠道，提供便捷的推送方法
 */
@Slf4j
@Service
public class NotifyManager {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private List<NotifyChannel> channels;

    /**
     * 发送消息到所有启用的渠道
     */
    public void send(NotifyMessage message) {
        if (channels == null || channels.isEmpty()) {
            log.debug("没有可用的推送渠道");
            return;
        }

        for (NotifyChannel channel : channels) {
            if (channel.isEnabled()) {
                try {
                    channel.send(message);
                    log.debug("推送成功 [{}]", channel.getName());
                } catch (Exception e) {
                    log.warn("推送失败 [{}]: {}", channel.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 发送任务执行结果通知
     *
     * @param jobName      任务名称
     * @param status       执行状态 (1=成功, 0=失败)
     * @param triggerType  触发类型 (CRON/MANUAL)
     * @param startTime    执行开始时间
     * @param endTime      执行结束时间
     * @param duration     执行耗时(毫秒)
     * @param errorMsg     错误信息
     */
    public void sendTaskResult(String jobName, int status, String triggerType,
                               LocalDateTime startTime, LocalDateTime endTime,
                               long duration, String errorMsg) {
        // 构建内容
        String content = buildTaskResultContent(jobName, status, triggerType, startTime, endTime, duration, errorMsg);

        // 构建扩展参数
        Map<String, Object> extras = new HashMap<>();
        extras.put("jobName", jobName);
        extras.put("status", status);
        extras.put("triggerType", triggerType);
        extras.put("startTime", startTime);
        extras.put("endTime", endTime);
        extras.put("duration", duration);
        extras.put("errorMsg", errorMsg);

        NotifyMessage message = NotifyMessage.builder()
                .title("定时任务执行通知")
                .type(NotifyType.TASK_RESULT)
                .content(content)
                .extras(extras)
                .build();

        send(message);
    }

    /**
     * 发送测试消息
     */
    public void sendTest() {
        String content = buildTestContent();

        NotifyMessage message = NotifyMessage.builder()
                .title("Webhook 测试消息")
                .type(NotifyType.INFO)
                .content(content)
                .build();

        send(message);
    }

    /**
     * 构建任务执行结果内容（通用格式，各渠道自行转换）
     */
    private String buildTaskResultContent(String jobName, int status, String triggerType,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          long duration, String errorMsg) {
        StringBuilder sb = new StringBuilder();

        // 任务名称
        sb.append("任务名称：").append(jobName).append("\n");

        // 执行状态
        sb.append("执行状态：").append(status == 1 ? "✅ 成功" : "❌ 失败").append("\n");

        // 触发类型
        String triggerLabel = "MANUAL".equals(triggerType) ? "手动触发" : "CRON定时";
        sb.append("触发类型：").append(triggerLabel).append("\n");

        // 开始时间
        if (startTime != null) {
            sb.append("开始时间：").append(startTime.format(DATE_TIME_FORMATTER)).append("\n");
        }

        // 结束时间
        if (endTime != null) {
            sb.append("结束时间：").append(endTime.format(DATE_TIME_FORMATTER)).append("\n");
        }

        // 执行耗时
        sb.append("执行耗时：").append(formatDuration(duration)).append("\n");

        // 错误信息
        if (status == 0 && errorMsg != null && !errorMsg.isBlank()) {
            String truncatedError = errorMsg.length() > 500 ? errorMsg.substring(0, 500) + "..." : errorMsg;
            sb.append("错误信息：").append(truncatedError);
        }

        return sb.toString();
    }

    /**
     * 构建测试消息内容
     */
    private String buildTestContent() {
        return "来源：BC体育数据管理系统\n" +
                "状态：✅ 连接成功\n" +
                "说明：此消息用于验证推送配置是否正确";
    }

    /**
     * 格式化耗时
     */
    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }
}
