package com.bcsport.admin.config;

import com.bcsport.admin.shiro.BCryptCredentialsMatcher;
import com.bcsport.admin.shiro.UserRealm;
import com.bcsport.admin.service.ConfigService;
import jakarta.servlet.Filter;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public DefaultWebSecurityManager securityManager(UserRealm userRealm, ConfigService configService,
                                                     org.springframework.data.redis.core.RedisTemplate<String, byte[]> redisTemplate) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

        // 设置 BCrypt 凭证匹配器（支持渐进式迁移）
        userRealm.setCredentialsMatcher(new BCryptCredentialsMatcher());

        // F32: 启用授权缓存——之前每次 @RequiresPermissions 都执行 doGetAuthorizationInfo
        // （含一次 getByUsername 查库），同一请求最多打 3 次库。开启后整个方法按 principal
        // 缓存（默认 MemoryConstrainedCacheManager，LRU 淘汰），权限变更时调
        // userRealm.clearCachedAuthorizationInfo() 失效（见 UserServiceImpl.evictUser 联动）。
        userRealm.setCachingEnabled(true);
        userRealm.setAuthorizationCachingEnabled(true);
        userRealm.setAuthorizationCacheName("authorizationCache");

        // 设置Realm
        securityManager.setRealm(userRealm);

        // F33: 接入 RedisSessionDAO——此前纯内存 session，重启全员掉线
        securityManager.setSessionManager(sessionManager(configService, redisTemplate));

        // TODO: RememberMe 功能暂时关闭（Redis 连接问题）
        // securityManager.setRememberMeManager(rememberMeManager());

        return securityManager;
    }

    /**
     * 配置 SessionManager（重写getSession，session不存在时返回null而非抛异常）
     */
    @Bean
    public DefaultWebSessionManager sessionManager(ConfigService configService,
                                                   org.springframework.data.redis.core.RedisTemplate<String, byte[]> redisTemplate) {
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
        // F33: Redis 会话持久化，重启不再全员掉线
        sessionManager.setSessionDAO(new com.bcsport.admin.shiro.RedisSessionDAO(redisTemplate, configService));
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
        // SameSite 从配置读取，默认 STRICT：跨站请求(含跨站导航)一律不携带会话 Cookie，
        // CSRF 无攻击面，替代已移除的 CsrfFilter(其 URL 模式带 context-path 前缀从未生效)。
        // 副作用：从外部链接首次点入系统会显示未登录，站内刷新即可，内部系统可接受。
        String sameSite = configService.getString("security.cookie.sameSite", "STRICT");
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
        // /images/** 映射的是用户上传的业务文件(导入Excel/打印物料)，不是构建产物：
        // 匿名可达等于按 URL 猜取业务数据，改为需登录(同源 <img> 会自动带会话Cookie，页面展示不受影响)
        filterChainDefinitionMap.put("/images/**", "spaAuth");
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        // 接口文档属于内部结构信息，纳入认证(登录后仍可查看，不再匿名暴露全部接口)
        filterChainDefinitionMap.put("/doc.html", "spaAuth");
        filterChainDefinitionMap.put("/webjars/**", "spaAuth");
        filterChainDefinitionMap.put("/v2/api-docs", "spaAuth");
        filterChainDefinitionMap.put("/swagger-resources/**", "spaAuth");
        // 贴纸打印 Agent 端点：仅对 C# 客户端使用的 4 个端点放行（其余 agent/print 端点仍走 spaAuth 供管理后台使用）
        // 由 AgentApiKeyInterceptor 校验 X-API-Key
        filterChainDefinitionMap.put("/api/agent/register", "anon");
        filterChainDefinitionMap.put("/api/agent/heartbeat", "anon");
        filterChainDefinitionMap.put("/api/print/pull", "anon");
        filterChainDefinitionMap.put("/api/print/result", "anon");
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
}
