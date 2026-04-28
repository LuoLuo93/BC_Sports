package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 认证控制器
 */
@Slf4j
@Controller
public class AuthController {
    
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
    public Result<?> doLogin(@RequestParam String username, 
                            @RequestParam String password) {
        try {
            Subject subject = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken(username, password);
            subject.login(token);
            
            // 登录成功，Shiro 会自动管理身份信息存储到 Session )
            log.info("用户登录成功: {}", username);
            return Result.success("登录成功", null);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return Result.error("用户名或密码错误");
        }
    }
    
    /**
     * 登出
     */
    @PostMapping("/doLogout")
    @ResponseBody
    public Result<?> doLogout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null) {
            String username = (String) subject.getPrincipal();
            // subject.logout() 会自动失效会话并清除身份信息，包括相关的 Cookie
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
     * 品牌管理页面
     */
    @GetMapping("/bi/brand")
    public String brandPage() {
        return "bi/brand";
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
        return "bi/region";
    }

    /**
     * 渠道类型管理页面
     */
    @GetMapping("/bi/channel-type")
    public String channelTypePage() {
        return "bi/channel-type";
    }

    /**
     * 渠道性质管理页面
     */
    @GetMapping("/bi/channel-nature")
    public String channelNaturePage() {
        return "bi/channel-nature";
    }

    /**
     * 实体渠道配置页面
     */
    @GetMapping("/bi/entity-channel")
    public String entityChannelPage() {
        return "bi/entity-channel";
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
     * IHR入职排除页面
     */
    @GetMapping("/ihr/onboarding-exclusion")
    public String onboardingExclusionPage() {
        return "ihr/onboarding-exclusion";
    }

    /**
     * IHR离职排除页面
     */
    @GetMapping("/ihr/leaving-exclusion")
    public String leavingExclusionPage() {
        return "ihr/leaving-exclusion";
    }

}
