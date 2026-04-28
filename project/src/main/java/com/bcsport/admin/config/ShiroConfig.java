package com.bcsport.admin.config;

import com.bcsport.admin.shiro.UserRealm;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;

import javax.servlet.Filter;
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
    public DefaultWebSecurityManager securityManager(UserRealm userRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        
        // 设置哈希凭证匹配器（MD5 + 2次迭代）
        userRealm.setCredentialsMatcher(hashedCredentialsMatcher());
        
        // 设置Realm
        securityManager.setRealm(userRealm);
        return securityManager;
    }
    
    /**
     * 配置密码匹配器
     */
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
        // 设置加密算法
        matcher.setHashAlgorithmName("md5");
        // 设置加密迭代次数（与PasswordUtil保持一致）
        matcher.setHashIterations(2);
        // 是否存储为16进制
        matcher.setStoredCredentialsHexEncoded(true);
        return matcher;
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
        // 可以在这里添加自定义过滤器，如JWT过滤器
        shiroFilterFactoryBean.setFilters(filters);
        
        // 配置路径过滤规则
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 公开路径
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/doLogin", "anon");
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
        
        // 需要认证的路径
        filterChainDefinitionMap.put("/**", "authc");
        
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
     * 配置ShiroDialect，用于Thymeleaf界面使用shiro标签
     */
    @Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }
}
