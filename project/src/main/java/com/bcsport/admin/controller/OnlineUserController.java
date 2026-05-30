package com.bcsport.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在线用户管理
 */
@Slf4j
@RestController
@RequestMapping("/api/online-user")
public class OnlineUserController {

    @Autowired
    private AuthCacheService authCacheService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private UserService userService;

    /**
     * 在线用户列表
     */
    @GetMapping("/list")
    @RequiresPermissions("system:online:list")
    public Result<?> list() {
        Map<String, String> sessionMap = authCacheService.scanSessionKeys();
        List<OnlineUserVO> list = new ArrayList<>();

        if (sessionMap.isEmpty()) {
            return Result.success(list);
        }

        // 批量查询用户信息，避免 N+1
        List<String> usernames = new ArrayList<>(sessionMap.keySet());
        Map<String, User> userMap = userService.list(
                new LambdaQueryWrapper<User>().in(User::getUsername, usernames)
        ).stream().collect(Collectors.toMap(User::getUsername, u -> u));

        for (Map.Entry<String, String> entry : sessionMap.entrySet()) {
            String username = entry.getKey();
            String sessionId = entry.getValue();

            Session session = null;
            try {
                session = sessionManager.getSession(new DefaultSessionKey(sessionId));
            } catch (Exception ignored) {
            }

            if (session == null) {
                // Session已过期，清理Redis中的映射
                authCacheService.removeUserSession(username);
                continue;
            }

            User user = userMap.get(username);
            OnlineUserVO vo = new OnlineUserVO();
            vo.setUsername(username);
            vo.setUserId(user != null ? user.getId() : "");
            vo.setNickname(user != null ? user.getNickname() : "");
            vo.setSessionId(sessionId);
            vo.setLoginTime(session.getStartTimestamp() != null ? session.getStartTimestamp().getTime() : null);
            vo.setLastAccessTime(session.getLastAccessTime() != null ? session.getLastAccessTime().getTime() : null);
            vo.setHost(session.getHost() != null ? session.getHost() : "");
            vo.setTimeout(session.getTimeout());
            list.add(vo);
        }

        return Result.success(list);
    }

    /**
     * 强制下线指定用户
     */
    @PostMapping("/kick/{username}")
    @RequiresPermissions("system:online:kick")
    public Result<?> kick(@PathVariable String username) {
        String sessionId = authCacheService.getActiveSessionId(username);
        if (sessionId != null) {
            try {
                Session session = sessionManager.getSession(new DefaultSessionKey(sessionId));
                if (session != null) {
                    session.stop();
                }
            } catch (Exception e) {
                log.warn("踢下线用户失败, username={}", username, e);
            }
            authCacheService.removeUserSession(username);
            log.info("用户 {} 已被强制下线", username);
        }
        return Result.success("用户已强制下线");
    }

    /**
     * 在线用户VO
     */
    @Data
    static class OnlineUserVO {
        private String username;
        private String userId;
        private String nickname;
        private String sessionId;
        private Long loginTime;
        private Long lastAccessTime;
        private String host;
        private long timeout;
    }
}