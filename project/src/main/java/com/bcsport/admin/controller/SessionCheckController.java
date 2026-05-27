package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.Dept;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.mapper.DeptMapper;
import com.bcsport.admin.mapper.MenuMapper;
import com.bcsport.admin.mapper.UserRoleMapper;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会话状态检查（用于前端轮询检测账号被踢出 + 获取当前用户信息）
 */
@Slf4j
@RestController
@RequestMapping("/api/session")
public class SessionCheckController {

    @Autowired
    private AuthCacheService authCacheService;

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private MenuMapper menuMapper;

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

    @GetMapping("/info")
    public Result<?> sessionInfo() {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return Result.unauthorized("未登录");
        }

        String username = (String) subject.getPrincipal();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        List<String> permissions;
        if ("admin".equals(username)) {
            permissions = java.util.Collections.singletonList("*");
        } else {
            List<String> cached = authCacheService.getPermissions(user.getId());
            if (cached != null) {
                permissions = cached;
            } else {
                permissions = menuMapper.getPermissionsByUserId(user.getId());
                authCacheService.putPermissions(user.getId(), permissions);
            }
            Set<String> permissionSet = permissions.stream()
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .collect(Collectors.toSet());
            permissions = new java.util.ArrayList<>(permissionSet);
        }

        Map<String, Object> info = new HashMap<>();
        info.put("username", username);
        info.put("nickname", user.getNickname());
        info.put("userId", user.getId());
        String deptName = "";
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) deptName = dept.getDeptName();
        }
        info.put("deptName", deptName);
        info.put("permissions", permissions);
        return Result.success(info);
    }
}
