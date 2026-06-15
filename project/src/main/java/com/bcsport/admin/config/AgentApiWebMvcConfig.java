package com.bcsport.admin.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 AgentApiKeyInterceptor，仅作用于 C# 客户端使用的 4 个端点
 * （register/heartbeat/pull/result）。管理后台使用的 list/detail/tasks/create-tasks
 * 仍走 Shiro 会话认证，不受此拦截器影响。
 *
 * 同时注册带与不带 context-path(/bcsports) 的两种模式，规避不同版本路径匹配差异。
 */
@Configuration
public class AgentApiWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AgentApiKeyInterceptor agentApiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agentApiKeyInterceptor)
                .addPathPatterns(
                        "/api/agent/register", "/api/agent/heartbeat",
                        "/api/print/pull", "/api/print/result",
                        "/bcsports/api/agent/register", "/bcsports/api/agent/heartbeat",
                        "/bcsports/api/print/pull", "/bcsports/api/print/result"
                );
    }
}
