package com.bcsport.admin.config;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 贴纸打印 Agent 端点的共享密钥校验。
 * 端点已在 Shiro 中以 anon 放行（仅 register/heartbeat/pull/result 4 个），
 * 这里校验请求头 X-API-Key 与配置 agent.api-key 是否一致。
 *
 * 读取优先级：数据库 bc_sports_sys_config（ConfigService 缓存，界面修改即时生效）
 *            → application.yml（@Value 兜底）。
 * 两者均为空时拒绝访问（fail-closed）：本地联调需在配置文件或数据库里给 agent.api-key 赋任意值。
 */
@Slf4j
@Component
public class AgentApiKeyInterceptor implements HandlerInterceptor {

    private static final String HEADER = "X-API-Key";
    private static final String CONFIG_KEY = "agent.api-key";

    @Autowired
    private ConfigService configService;

    @Value("${agent.api-key:}")
    private String fallbackApiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            // fail-closed：生产漏配 AGENT_API_KEY 时端点必须不可用，而不是匿名开放
            log.warn("Agent 端点访问被拒绝：agent.api-key 未配置（数据库与配置文件均为空），uri={}",
                    request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error(503, "服务端未配置 Agent API 密钥，请联系管理员")));
            return false;
        }
        String provided = request.getHeader(HEADER);
        if (provided != null && MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("无效的 API Key")));
        return false;
    }

    /**
     * 优先从数据库缓存读取，回退到 application.yml 配置。
     */
    private String resolveApiKey() {
        String dbValue = configService.getString(CONFIG_KEY);
        if (StringUtils.hasText(dbValue)) {
            return dbValue;
        }
        return fallbackApiKey;
    }
}
