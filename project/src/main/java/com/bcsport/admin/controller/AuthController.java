package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.config.csrf.CsrfFilter;
import com.bcsport.admin.dto.LoginDTO;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.UserService;
import com.bcsport.admin.util.BCryptPasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 认证控制器
 */
@Slf4j
@Controller
public class AuthController {

    @Autowired
    private AuthCacheService authCacheService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;
    
    /**
     * 登录页面 - 返回 Vue SPA 入口
     */
    @GetMapping("/login")
    public String loginPage() {
        log.info("访问登录页面");
        return "forward:/index.html";
    }
    
    /**
     * 处理登录请求
     */
    @PostMapping("/doLogin")
    @ResponseBody
    @OperLog(module = "系统认证", operation = "用户登录", saveParams = false)
    public Result<?> doLogin(@RequestBody LoginDTO loginDTO) {
        String username = loginDTO.getUsername();

        // 验证码校验
        boolean captchaEnabled = configService.getBoolean("security.captchaEnabled", true);
        if (captchaEnabled) {
            String captchaKey = loginDTO.getCaptchaKey();
            String captchaCode = loginDTO.getCaptchaCode();
            if (captchaKey == null || captchaCode == null) {
                return Result.error("请输入验证码");
            }
            String cachedCode = authCacheService.getCaptchaAndDelete(captchaKey);
            if (cachedCode == null) {
                return Result.error("验证码已过期，请刷新验证码");
            }
            if (!cachedCode.equalsIgnoreCase(captchaCode)) {
                return Result.error("验证码错误");
            }
        }

        // 检查账号是否被锁定
        long lockSeconds = authCacheService.getLockRemainSeconds(username);
        if (lockSeconds > 0) {
            long minutes = (lockSeconds + 59) / 60;
            return Result.error("账号已锁定，请稍后再试");
        }

        try {
            Subject subject = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken(username, loginDTO.getPassword());
            // TODO: RememberMe 功能暂时关闭（Redis 连接问题）
            // if (Boolean.TRUE.equals(loginDTO.getRememberMe())) {
            //     token.setRememberMe(true);
            // }
            subject.login(token);

            // 登录成功，清除失败计数
            authCacheService.clearLoginFailures(username);

            // 踢掉该用户的旧会话
            String currentSessionId = subject.getSession().getId().toString();
            String oldSessionId = authCacheService.getActiveSessionId(username);
            if (oldSessionId != null && !oldSessionId.equals(currentSessionId)) {
                try {
                    SessionManager sessionManager =
                            ((DefaultWebSecurityManager) SecurityUtils.getSecurityManager()).getSessionManager();
                    Session existingSession = sessionManager.getSession(new DefaultSessionKey(oldSessionId));
                    existingSession.stop();
                    log.info("踢掉用户 {} 的旧会话: {}", username, oldSessionId);
                } catch (Exception e) {
                    log.warn("踢掉旧会话失败, username={}, oldSessionId={}: {}", username, oldSessionId, e.getMessage());
                }
            }

            // 绑定新会话
            authCacheService.bindUserSession(username, currentSessionId);

            // 检查是否需要迁移密码（从 MD5 到 BCrypt）
            // 直接查库判断 passwordNew 字段，不依赖 Session 属性传递（认证阶段 Session 可能不可用）
            try {
                User user = userService.getByUsername(username);
                if (user != null && (user.getPasswordNew() == null || user.getPasswordNew().isEmpty())) {
                    String newBCryptPassword = BCryptPasswordUtil.encrypt(loginDTO.getPassword());
                    userService.migratePassword(user.getId(), newBCryptPassword);
                    log.info("用户 {} 密码已从 MD5 迁移到 BCrypt", username);
                }
            } catch (Exception e) {
                log.error("用户 {} 密码迁移失败: {}", username, e.getMessage());
                // 迁移失败不影响登录
            }

            log.info("用户登录成功: {}", username);
            return Result.success("登录成功", null);
        } catch (Exception e) {
            // 登录失败，记录失败次数
            long failCount = authCacheService.recordLoginFailure(username);
            int maxRetry = configService.getInt("security.loginMaxRetry", 5);
            int lockMinutes = configService.getInt("security.loginLockMinutes", 30);

            if (failCount >= maxRetry) {
                authCacheService.lockAccount(username, lockMinutes);
                log.warn("用户 {} 连续登录失败 {} 次，已锁定 {} 分钟", username, failCount, lockMinutes);
                return Result.error("登录失败次数过多，账号已暂时锁定");
            }

            log.error("登录失败: username={}, 剩余尝试次数={}", username, maxRetry - failCount);
            return Result.error("用户名或密码错误");
        }
    }
    
    /**
     * 登出
     */
    @PostMapping("/doLogout")
    @ResponseBody
    @OperLog(module = "系统认证", operation = "用户登出", saveParams = false)
    public Result<?> doLogout(jakarta.servlet.http.HttpServletResponse response) {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null) {
            String username = (String) subject.getPrincipal();
            if (username != null) {
                authCacheService.removeUserSession(username);
            }
            // 清除 RememberMe Cookie
            jakarta.servlet.http.Cookie rmCookie = new jakarta.servlet.http.Cookie("rememberMe", "");
            rmCookie.setPath("/");
            rmCookie.setMaxAge(0);
            rmCookie.setHttpOnly(true);
            response.addCookie(rmCookie);

            subject.logout();
            log.info("用户登出系统: {}", username);
        }
        return Result.success("登出成功", null);
    }
    
    /**
     * 首页 - 返回 Vue SPA 入口
     */
    @GetMapping({"/", "/index"})
    public String index() {
        return "forward:/index.html";
    }
    
    /**
     * 所有页面路由 - 统一返回 Vue SPA 入口，由 Vue Router 处理前端路由
     */
    @GetMapping({
        "/system/menu", "/system/user", "/system/role", "/system/dept", "/system/dict",
        "/system/log", "/system/online-user",
        "/bi/management", "/bi/brand", "/bi/region", "/bi/channel-type", "/bi/channel-nature",
        "/bi/entity-channel", "/bi/entity-channel/form", "/bi/erp-shop", "/bi/erp-warehouse", "/bi/erp-customer",
        "/bi/erp-store", "/bi/first-add", "/bi/retail-supervisor", "/bi/goods-data",
        "/statistics", "/report", "/message", "/help", "/profile", "/settings", "/403",
        "/monitor/schedule", "/monitor/system",
        "/sticker/print", "/sticker/print/{orderId}", "/sticker/data", "/sticker/brand-template", "/sticker/agent", "/sticker/field-mapping",
        "/qywx/customer-tag", "/qywx/follow-user", "/qywx/group-chat", "/qywx/moment",
        "/nxcrm/customer-tag", "/nxcrm/member-tag",
        "/ihr/onboarding-exclusion", "/ihr/leaving-exclusion",
        "/ihr/onboarding-management", "/ihr/adjustment-management", "/ihr/leaving-management", "/ihr/employee-management",
        "/erp/employee-management"
    })
    public String vuePage() {
        return "forward:/index.html";
    }

    /**
     * 获取 CSRF Token
     *
     * 生成并返回 CSRF Token，存储到 Shiro Session 中
     * 注意：必须使用 Shiro Session 而非 Servlet Session，避免 Servlet 容器覆盖 JSESSIONID Cookie
     */
    @GetMapping("/api/csrf")
    @ResponseBody
    public Result<String> getCsrfToken() {
        try {
            Session session = SecurityUtils.getSubject().getSession();
            String token = UUID.randomUUID().toString();
            session.setAttribute(CsrfFilter.CSRF_TOKEN_ATTR, token);
            return Result.success(token);
        } catch (Exception e) {
            log.warn("获取 CSRF Token 失败: {}", e.getMessage());
            // 返回一个新 Token，不存储到 Session
            return Result.success(UUID.randomUUID().toString());
        }
    }

}
