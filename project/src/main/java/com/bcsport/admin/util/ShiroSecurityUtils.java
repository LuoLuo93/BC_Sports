package com.bcsport.admin.util;

import com.bcsport.admin.entity.User;
import com.bcsport.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 */
@Slf4j
@Component
public class ShiroSecurityUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    // 可配置的默认用户名
    private static String DEFAULT_USERNAME = "system";

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    /**
     * 设置默认用户名
     */
    public static void setDefaultUsername(String defaultUsername) {
        DEFAULT_USERNAME = defaultUsername;
    }

    /**
     * 获取默认用户名
     */
    private static String getDefaultUsername() {
        return DEFAULT_USERNAME;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Subject subject = getSubject();
        if (subject != null && (subject.isAuthenticated() || subject.isRemembered())) {
            Object principal = subject.getPrincipal();
            if (principal != null) {
                return principal.toString();
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     * 通过 Principal 中的用户名查询数据库获取真实用户ID
     */
    public static String getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null || getDefaultUsername().equals(username)) {
            return null;
        }
        try {
            if (applicationContext != null) {
                UserService userService = applicationContext.getBean(UserService.class);
                User user = userService.getByUsername(username);
                return user != null ? user.getId() : null;
            }
        } catch (Exception e) {
            log.warn("获取用户ID失败, username={}", username, e);
        }
        return null;
    }

    /**
     * 获取当前Subject
     */
    public static Subject getSubject() {
        try {
            return SecurityUtils.getSubject();
        } catch (UnavailableSecurityManagerException e) {
            log.warn("SecurityManager不可用", e);
            return null;
        }
    }

    /**
     * 判断用户是否已登录
     */
    public static boolean isAuthenticated() {
        Subject subject = getSubject();
        return subject != null && subject.isAuthenticated();
    }

    /**
     * 判断用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }
        Subject subject = getSubject();
        return subject != null && subject.isPermitted(permission);
    }

    /**
     * 判断用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        Subject subject = getSubject();
        return subject != null && subject.hasRole(role);
    }
}
