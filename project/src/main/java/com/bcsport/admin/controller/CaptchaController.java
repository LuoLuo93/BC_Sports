package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.util.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private AuthCacheService authCacheService;

    @GetMapping
    public Result<Map<String, String>> generate() {
        String code = CaptchaUtil.generateCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        authCacheService.putCaptcha(key, code);

        Map<String, String> data = new HashMap<>();
        data.put("captchaKey", key);
        try {
            data.put("captchaImage", CaptchaUtil.generateImageBase64(code));
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            return Result.error("生成验证码失败");
        }
        return Result.success(data);
    }
}
