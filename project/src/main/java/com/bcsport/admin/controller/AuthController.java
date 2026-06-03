package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.LoginDTO;
import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.ConfigService;
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
    
    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        log.info("访问登录页面");
        return "common/login";
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
     * 首页
     */
    @GetMapping({"/", "/index"})
    public String index() {
        return "common/index";
    }
    
    /**
     * 菜单管理页面
     */
    @GetMapping("/system/menu")
    public String menuPage() {
        return "system/menu";
    }
    
    /**
     * 用户管理页面
     */
    @GetMapping("/system/user")
    public String userPage() {
        return "system/user";
    }
    
    /**
     * 角色管理页面
     */
    @GetMapping("/system/role")
    public String rolePage() {
        return "system/role";
    }

    /**
     * 基础数据管理页面（品牌/地区/渠道类型/渠道性质）
     */
    @GetMapping("/bi/management")
    public String biManagementPage() {
        return "bi/bi-management";
    }

    /**
     * 品牌管理页面 - 重定向到基础数据管理
     */
    @GetMapping("/bi/brand")
    public String brandPage() {
        return "redirect:/bi/management?tab=brand";
    }

    /**
     * 部门管理页面
     */
    @GetMapping("/system/dept")
    public String deptPage() {
        return "system/dept";
    }
    
    /**
     * 数据字典页面
     */
    @GetMapping("/system/dict")
    public String dictPage() {
        return "system/dict";
    }
    
    /**
     * 数据统计页面
     */
    @GetMapping("/statistics")
    public String statisticsPage() {
        return "statistics/statistics";
    }
    
    /**
     * 报表管理页面
     */
    @GetMapping("/report")
    public String reportPage() {
        return "statistics/report";
    }
    
    /**
     * 消息通知页面
     */
    @GetMapping("/message")
    public String messagePage() {
        return "message/message";
    }
    
    /**
     * 帮助中心页面
     */
    @GetMapping("/help")
    public String helpPage() {
        return "help";
    }
    
    /**
     * 个人中心页面
     */
    @GetMapping("/profile")
    public String profilePage() {
        return "user/profile";
    }
    
    /**
     * 系统设置页面
     */
    @GetMapping("/settings")
    public String settingsPage() {
        return "settings";
    }
    
    /**
     * 403页面
     */
    @GetMapping("/403")
    public String forbidden() {
        return "403";
    }

    @GetMapping("/bi/region")
    public String region() {
        return "redirect:/bi/management?tab=region";
    }

    /**
     * 渠道类型管理页面 - 重定向到基础数据管理
     */
    @GetMapping("/bi/channel-type")
    public String channelTypePage() {
        return "redirect:/bi/management?tab=channelType";
    }

    /**
     * 渠道性质管理页面 - 重定向到基础数据管理
     */
    @GetMapping("/bi/channel-nature")
    public String channelNaturePage() {
        return "redirect:/bi/management?tab=channelNature";
    }

    /**
     * 实体渠道配置页面
     */
    @GetMapping("/bi/entity-channel")
    public String entityChannelPage() {
        return "bi/entity-channel";
    }

    @GetMapping("/bi/entity-channel/form")
    public String entityChannelFormPage(@RequestParam(required = false) String id, org.springframework.ui.Model model) {
        model.addAttribute("editId", id != null ? id : "");
        return "bi/entity-channel-form";
    }

    /**
     * ERP 店铺管理页面
     */
    @GetMapping("/bi/erp-shop")
    public String erpShopPage() {
        return "bi/erp-shop";
    }

    /**
     * ERP 仓库管理页面
     */
    @GetMapping("/bi/erp-warehouse")
    public String erpWarehousePage() {
        return "bi/erp-warehouse";
    }

    /**
     * ERP 客户管理页面
     */
    @GetMapping("/bi/erp-customer")
    public String erpCustomerPage() {
        return "bi/erp-customer";
    }

    /**
     * 定时任务管理页面
     */
    @GetMapping("/monitor/schedule")
    public String schedulePage() {
        return "monitor/schedule";
    }

    /**
     * 企微客户标签管理页面
     */
    @GetMapping("/qywx/customer-tag")
    public String customerTagPage() {
        return "qywx/customer-tag";
    }

    /**
     * IHR入职排除页面
     */
    @GetMapping("/ihr/onboarding-exclusion")
    public String onboardingExclusionPage(org.springframework.ui.Model model) {
        model.addAttribute("exclusionType", 1);
        model.addAttribute("pageTitle", "入职排除");
        return "ihr/exclusion";
    }

    /**
     * IHR离职排除页面
     */
    @GetMapping("/ihr/leaving-exclusion")
    public String leavingExclusionPage(org.springframework.ui.Model model) {
        model.addAttribute("exclusionType", 2);
        model.addAttribute("pageTitle", "离职排除");
        return "ihr/exclusion";
    }

    /**
     * IHR人员入职管理页面
     */
    @GetMapping("/ihr/onboarding-management")
    public String onboardingManagementPage() {
        return "ihr/onboarding-management";
    }

    /**
     * IHR人员调整管理页面
     */
    @GetMapping("/ihr/adjustment-management")
    public String adjustmentManagementPage() {
        return "ihr/adjustment-management";
    }

    /**
     * IHR人员离职管理页面
     */
    @GetMapping("/ihr/leaving-management")
    public String leavingManagementPage() {
        return "ihr/leaving-management";
    }

    /**
     * IHR人员管理页面（入职/调整/离职三合一）
     */
    @GetMapping("/ihr/employee-management")
    public String ihrEmployeeManagementPage() {
        return "ihr/employee-management";
    }

    /**
     * ERP人员管理页面
     */
    @GetMapping("/erp/employee-management")
    public String erpEmployeeManagementPage() {
        return "erp/employee-management";
    }

}
