package com.bcsport.admin.config;

import com.bcsport.admin.shiro.BCryptCredentialsMatcher;
import com.bcsport.admin.shiro.UserRealm;
import com.bcsport.admin.service.ConfigService;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;

import javax.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro 配置类
 */
@Configuration
public class ShiroConfig {
    
    /**
     * 配置SecurityManager
     */
    @Bean
    public DefaultWebSecurityManager securityManager(UserRealm userRealm, ConfigService configService) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

        // 设置 BCrypt 凭证匹配器（支持渐进式迁移）
        userRealm.setCredentialsMatcher(new BCryptCredentialsMatcher());

        // 设置Realm
        securityManager.setRealm(userRealm);

        // 配置 SessionManager
        securityManager.setSessionManager(sessionManager(configService));

        // TODO: RememberMe 功能暂时关闭（Redis 连接问题）
        // securityManager.setRememberMeManager(rememberMeManager());

        return securityManager;
    }

    /**
     * 配置 SessionManager（重写getSession，session不存在时返回null而非抛异常）
     */
    @Bean
    public DefaultWebSessionManager sessionManager(ConfigService configService) {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager() {
            @Override
            public Session getSession(SessionKey key) throws SessionException {
                try {
                    return super.getSession(key);
                } catch (UnknownSessionException e) {
                    return null;
                }
            }
        };
        sessionManager.setSessionIdCookie(sessionIdCookie(configService));
        int timeoutMinutes = configService.getInt("security.sessionTimeout", 30);
        sessionManager.setGlobalSessionTimeout(timeoutMinutes * 60 * 1000L);
        sessionManager.setDeleteInvalidSessions(true);
        return sessionManager;
    }

    /**
     * 配置 Session Cookie
     */
    @Bean
    public SimpleCookie sessionIdCookie(ConfigService configService) {
        SimpleCookie cookie = new SimpleCookie("JSESSIONID");
        cookie.setHttpOnly(true);
        cookie.setSecure(configService.getBoolean("security.cookie.secure", false));
        cookie.setMaxAge(-1); // 浏览器关闭即失效
        // SameSite 从配置读取，开发环境默认 LAX，生产环境设为 STRICT
        String sameSite = configService.getString("security.cookie.sameSite", "LAX");
        cookie.setSameSite(Cookie.SameSiteOptions.valueOf(sameSite.toUpperCase()));
        return cookie;
    }

    /**
     * 配置 RememberMe Cookie
     */
    @Bean
    public SimpleCookie rememberMeCookie(ConfigService configService) {
        SimpleCookie cookie = new SimpleCookie("rememberMe");
        cookie.setHttpOnly(true);
        cookie.setSecure(configService.getBoolean("security.cookie.secure", false));
        cookie.setMaxAge(2592000); // 30天
        return cookie;
    }

    /**
     * 配置 RememberMe 管理器
     */
    @Bean
    public CookieRememberMeManager rememberMeManager(ConfigService configService) {
        CookieRememberMeManager manager = new CookieRememberMeManager();
        manager.setCookie(rememberMeCookie(configService));
        // 加密密钥：优先从环境变量读取，否则每次启动随机生成
        String envKey = System.getenv("SHIRO_REMEMBER_ME_KEY");
        byte[] cipherKey;
        if (envKey != null && !envKey.isEmpty()) {
            cipherKey = envKey.getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] key = new byte[16];
            new SecureRandom().nextBytes(key);
            cipherKey = key;
        }
        manager.setCipherKey(cipherKey);
        return manager;
    }

    /**
     * 配置ShiroFilterFactoryBean
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        
        // 添加自定义过滤器
        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("spaAuth", new SpaAuthFilter());
        shiroFilterFactoryBean.setFilters(filters);
        
        // 配置路径过滤规则
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 公开路径
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/doLogin", "anon");
        filterChainDefinitionMap.put("/api/config/public", "anon");
        filterChainDefinitionMap.put("/api/captcha", "anon");
        filterChainDefinitionMap.put("/assets/**", "anon");
        filterChainDefinitionMap.put("/static/**", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/images/**", "anon");
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/doc.html", "anon");
        filterChainDefinitionMap.put("/webjars/**", "anon");
        filterChainDefinitionMap.put("/v2/api-docs", "anon");
        filterChainDefinitionMap.put("/swagger-resources/**", "anon");
        // Actuator: 仅 health 端点允许匿名访问，其余需认证
        filterChainDefinitionMap.put("/actuator/health", "anon");
        filterChainDefinitionMap.put("/actuator/**", "spaAuth");

        // 需要认证的路径（使用 SpaAuthFilter 处理 API 请求返回 JSON 而非重定向，支持 Remember-Me）
        filterChainDefinitionMap.put("/**", "spaAuth");
        
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        
        // 设置登录页面
        shiroFilterFactoryBean.setLoginUrl("/login");
        // 设置登录成功后跳转的页面
        shiroFilterFactoryBean.setSuccessUrl("/index");
        // 设置未授权页码
        shiroFilterFactoryBean.setUnauthorizedUrl("/403");
        
        return shiroFilterFactoryBean;
    }
    
    /**
     * 创建Shiro授权属性源顾问（用于Shiro注解）
     * @param securityManager 安全管理员
     * @return 授权属性源顾问实例
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
    
    /**
     * 配置DefaultAdvisorAutoProxyCreator（用于Shiro注解）
     */
    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }

    /**
     * 注册 Shiro 过滤器到 Jakarta Servlet 容器（Spring Boot 3.x 兼容）
     */
    @Bean
    public FilterRegistrationBean<jakarta.servlet.Filter> shiroFilterRegistration(
            ShiroFilterFactoryBean shiroFilterFactoryBean) throws Exception {
        AbstractShiroFilter shiroFilter = (AbstractShiroFilter) shiroFilterFactoryBean.getObject();
        return ShiroJakartaBridge.register(shiroFilter);
    }

    /**
     * 配置ShiroDialect，用于Thymeleaf界面使用shiro标签
     */
    @Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }
}
