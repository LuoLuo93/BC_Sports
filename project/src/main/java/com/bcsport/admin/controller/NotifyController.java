package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.service.SysConfigService;
import com.bcsport.admin.service.notify.NotifyManager;
import com.bcsport.admin.service.notify.channel.WechatChannel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/notify")
@Api(tags = "消息通知管理")
public class NotifyController {

    @Autowired
    private NotifyManager notifyManager;

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 测试企微群机器人 Webhook
     *
     * 接收前端传入的 webhookUrl，临时保存到配置表后测试发送
     */
    @PostMapping("/test-webhook")
    @ApiOperation("测试企微群机器人Webhook")
    public Result<Void> testWebhook(@RequestBody Map<String, String> body) {
        String webhookUrl = body.get("webhookUrl");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return Result.error("Webhook URL 不能为空");
        }

        if (!webhookUrl.startsWith("https://qyapi.weixin.qq.com/")) {
            return Result.error("Webhook URL 格式不正确，应以 https://qyapi.weixin.qq.com/ 开头");
        }

        try {
            // 临时保存 webhook URL 到配置表（用于测试）
            Map<String, String> configUpdate = new HashMap<>();
            configUpdate.put("schedule.notify.webhookUrl", webhookUrl);
            sysConfigService.updateConfigs(configUpdate);

            // 发送测试消息
            notifyManager.sendTest();

            return Result.success();
        } catch (Exception e) {
            log.error("测试Webhook失败", e);
            return Result.error("发送失败: " + e.getMessage());
        }
    }
}
