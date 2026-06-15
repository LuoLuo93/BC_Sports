package com.bcsport.admin.config;

import com.bcsport.admin.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 贴纸打印 Agent 端点的共享密钥校验。
 * 端点已在 Shiro 中以 anon 放行（仅 register/heartbeat/pull/result 4 个），
 * 这里校验请求头 X-API-Key 与配置 agent.api-key 是否一致。
 * - agent.api-key 为空：放行（本地零配置可用）。
 * - agent.api-key 非空：请求头缺失或不符返回 401。
 */
@Component
public class AgentApiKeyInterceptor implements HandlerInterceptor {

    private static final String HEADER = "X-API-Key";

    @Value("${agent.api-key:}")
    private String apiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!StringUtils.hasText(apiKey)) {
            return true;
        }
        if (apiKey.equals(request.getHeader(HEADER))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("无效的 API Key")));
        return false;
    }
}
