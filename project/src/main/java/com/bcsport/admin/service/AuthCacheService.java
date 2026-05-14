package com.bcsport.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthCacheService {

    private static final String PERMISSION_KEY = "auth:perm:";
    private static final String ROLE_KEY = "auth:role:";
    private static final String MENU_KEY = "auth:menu:";
    private static final String SESSION_KEY = "auth:session:";
    private static final long TTL_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String userId) {
        try {
            Object cached = redisTemplate.opsForValue().get(PERMISSION_KEY + userId);
            if (cached != null) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis读取权限缓存失败, userId={}", userId, e);
        }
        return null;
    }

    public void putPermissions(String userId, List<String> permissions) {
        try {
            redisTemplate.opsForValue().set(PERMISSION_KEY + userId, permissions, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis写入权限缓存失败, userId={}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String userId) {
        try {
            Object cached = redisTemplate.opsForValue().get(ROLE_KEY + userId);
            if (cached != null) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis读取角色缓存失败, userId={}", userId, e);
        }
        return null;
    }

    public void putRoles(String userId, List<String> roles) {
        try {
            redisTemplate.opsForValue().set(ROLE_KEY + userId, roles, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis写入角色缓存失败, userId={}", userId, e);
        }
    }

    public void evictUser(String userId) {
        try {
            redisTemplate.delete(PERMISSION_KEY + userId);
            redisTemplate.delete(ROLE_KEY + userId);
            redisTemplate.delete(MENU_KEY + userId);
        } catch (Exception e) {
            log.warn("Redis清除用户缓存失败, userId={}", userId, e);
        }
    }

    // ==================== 会话唯一性管理 ====================

    public void bindUserSession(String username, String sessionId) {
        try {
            redisTemplate.opsForValue().set(SESSION_KEY + username, sessionId);
        } catch (Exception e) {
            log.warn("Redis绑定用户会话失败, username={}", username, e);
        }
    }

    public String getActiveSessionId(String username) {
        try {
            Object val = redisTemplate.opsForValue().get(SESSION_KEY + username);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            log.warn("Redis获取用户会话失败, username={}", username, e);
            return null;
        }
    }

    public void removeUserSession(String username) {
        try {
            redisTemplate.delete(SESSION_KEY + username);
        } catch (Exception e) {
            log.warn("Redis清除用户会话失败, username={}", username, e);
        }
    }

    public void evictAll() {
        try {
            scanAndDelete(PERMISSION_KEY);
            scanAndDelete(ROLE_KEY);
            scanAndDelete(MENU_KEY);
        } catch (Exception e) {
            log.warn("Redis清除全部权限缓存失败", e);
        }
    }

    private void scanAndDelete(String prefix) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        org.springframework.data.redis.core.Cursor<String> cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions().match(prefix + "*").count(100).build());
        while (cursor.hasNext()) {
            keys.add(cursor.next());
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
