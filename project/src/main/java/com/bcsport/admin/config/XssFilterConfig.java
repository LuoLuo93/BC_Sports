package com.bcsport.admin.config;

import com.bcsport.admin.config.xss.XssFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * XSS 过滤器配置
 *
 * 注册 XSS 过滤器，设置优先级和 URL 模式
 */
@Configuration
public class XssFilterConfig {

    /**
     * 注册 XSS 过滤器
     *
     * 优先级设置为 HIGHEST_PRECEDENCE + 1，在 Shiro 之前执行
     * 过滤所有请求，包括参数和请求体
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setEnabled(true);
        return registration;
    }
}
