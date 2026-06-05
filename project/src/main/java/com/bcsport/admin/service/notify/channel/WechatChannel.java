package com.bcsport.admin.service.notify.channel;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.notify.NotifyChannel;
import com.bcsport.admin.service.notify.NotifyMessage;
import com.bcsport.admin.service.notify.NotifyType;
import com.bcsport.admin.service.notify.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信群机器人推送渠道
 */
@Slf4j
@Component
public class WechatChannel implements NotifyChannel {

    private static final String CONFIG_KEY_WEBHOOK_URL = "schedule.notify.webhookUrl";

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 基础重试间隔（毫秒） */
    private static final long BASE_DELAY = 1000;

    @Autowired
    private ConfigService configService;

    @Override
    public String getName() {
        return "企微群机器人";
    }

    @Override
    public boolean isEnabled() {
        String webhookUrl = configService.getString(CONFIG_KEY_WEBHOOK_URL);
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    @Override
    public void send(NotifyMessage message) {
        String webhookUrl = configService.getString(CONFIG_KEY_WEBHOOK_URL);
        // isEnabled() 已判断过，这里直接使用
        try {
            String content = convertToMarkdown(message);
            // 使用重试机制发送消息
            RetryUtil.executeWithRetryVoid(() -> sendMarkdownMessage(webhookUrl, content), MAX_RETRIES, BASE_DELAY);
        } catch (Exception e) {
            log.error("企微群推送失败（已重试{}次）: {}", MAX_RETRIES, e.getMessage(), e);
            throw new RuntimeException("企微群推送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将通用消息转换为企微 Markdown 格式
     */
    private String convertToMarkdown(NotifyMessage message) {
        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append("### <font color=\"info\">").append(message.getTitle()).append("</font>\n");

        // 根据消息类型设置不同的颜色
        String color = getColorByType(message.getType());

        // 内容（按行处理）
        String content = message.getContent();
        if (content != null && !content.isBlank()) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.startsWith("执行状态：")) {
                    // 状态行特殊处理
                    if (line.contains("成功")) {
                        sb.append("> ").append(line.replace("成功", "<font color=\"warning\">✅ 成功</font>")).append("\n");
                    } else if (line.contains("失败")) {
                        sb.append("> ").append(line.replace("失败", "<font color=\"warning\">❌ 失败</font>")).append("\n");
                    } else {
                        sb.append("> <font color=\"").append(color).append("\">").append(line).append("</font>\n");
                    }
                } else if (line.startsWith("错误信息：")) {
                    // 错误信息用红色
                    sb.append("> <font color=\"warning\">").append(line).append("</font>\n");
                } else {
                    sb.append("> <font color=\"").append(color).append("\">").append(line).append("</font>\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 根据消息类型获取颜色
     */
    private String getColorByType(NotifyType type) {
        if (type == null) return "comment";
        switch (type) {
            case TASK_RESULT:
                return "comment";
            case ALERT:
                return "warning";
            case INFO:
                return "info";
            default:
                return "comment";
        }
    }

    /**
     * 发送 Markdown 消息到企微群机器人
     */
    private void sendMarkdownMessage(String webhookUrl, String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");

        Map<String, String> markdown = new HashMap<>();
        markdown.put("content", content);
        body.put("markdown", markdown);

        String jsonBody = JSONUtil.toJsonStr(body);
        log.debug("企微群推送请求: {}", jsonBody);

        HttpResponse response = HttpRequest.post(webhookUrl)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .timeout(10000)
                .execute();

        String responseBody = response.body();
        log.debug("企微群推送响应: {}", responseBody);

        if (response.getStatus() != 200) {
            throw new RuntimeException("HTTP请求失败，状态码: " + response.getStatus());
        }

        JSONObject result = JSONUtil.parseObj(responseBody);
        int errcode = result.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = result.getStr("errmsg");
            throw new RuntimeException("企微返回错误: " + errmsg);
        }

        log.info("企微群推送成功");
    }
}
