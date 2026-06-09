package com.bcsport.admin.config;

import com.bcsport.admin.config.csrf.CsrfFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * CSRF 过滤器配置
 *
 * 注册 CSRF 过滤器，设置优先级和 URL 模式
 */
@Configuration
public class CsrfFilterConfig {

    /**
     * 注册 CSRF 过滤器
     *
     * 优先级设置为 HIGHEST_PRECEDENCE + 2，在 XSS 过滤器之后执行
     * 过滤 API 请求和登录/登出请求
     * 排除文件上传接口（multipart/form-data 请求）
     */
    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CsrfFilter());
        registration.addUrlPatterns("/bcsports/api/*", "/bcsports/doLogin", "/bcsports/doLogout");
        registration.setName("csrfFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.setEnabled(true);
        return registration;
    }
}
