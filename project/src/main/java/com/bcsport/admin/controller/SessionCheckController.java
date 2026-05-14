package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.service.AuthCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话状态检查（用于前端轮询检测账号被踢出）
 */
@Slf4j
@RestController
@RequestMapping("/api/session")
public class SessionCheckController {

    @Autowired
    private AuthCacheService authCacheService;

    @GetMapping("/check")
    public Result<?> check() {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return Result.error("未登录");
        }

        String username = (String) subject.getPrincipal();
        String currentSessionId = subject.getSession().getId().toString();
        String activeSessionId = authCacheService.getActiveSessionId(username);

        if (activeSessionId != null && !activeSessionId.equals(currentSessionId)) {
            // 当前会话已不是最新会话，说明被踢了
            subject.logout();
            return Result.error("您的账号已在其他设备登录");
        }

        return Result.success("ok");
    }
}
