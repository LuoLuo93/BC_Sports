package com.bcsport.admin.task.ihr;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bcsport.admin.entity.ihr.IhrAccessToken;
import com.bcsport.admin.ihrmapper.IhrAccessTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * IHR360 API 统一客户端
 * 封装 OAuth Token 管理和 HTTP 请求
 */
@Slf4j
@Component
public class IhrApiClient {

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Value("${ihr.client-id}")
    private String clientId;

    @Value("${ihr.client-secret}")
    private String clientSecret;

    @Value("${ihr.api-base-url:https://openapi.ihr360.com}")
    private String baseUrl;

    @Value("${ihr.token-url:/openapi/oauth/token}")
    private String tokenUrl;

    private RestTemplate restTemplate;

    private final ReentrantLock tokenLock = new ReentrantLock();
    private volatile String accessToken;
    private volatile LocalDateTime tokenExpireTime;

    @Autowired
    private IhrAccessTokenMapper accessTokenMapper;

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        // 设置超时
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        restTemplate.setRequestFactory(factory);
        log.info("IhrApiClient 初始化完成, baseUrl: {}", baseUrl);

        // 从数据库加载已存在的 Token
        loadTokenFromDatabase();
    }

    private void loadTokenFromDatabase() {
        try {
            List<IhrAccessToken> tokens = accessTokenMapper.selectList(null);
            if (tokens != null && !tokens.isEmpty()) {
                IhrAccessToken token = tokens.get(0);
                this.accessToken = token.getAccessToken();
                // 假设 token 有效期为 2 小时，减去 5 分钟提前刷新
                if (token.getCreateTime() != null) {
                    this.tokenExpireTime = java.time.LocalDateTime.ofInstant(
                            token.getCreateTime().toInstant(),
                            java.time.ZoneId.systemDefault()
                        ).plusHours(2).minusMinutes(5);
                }
                log.info("从数据库加载 Token 完成");
            }
        } catch (Exception e) {
            log.warn("从数据库加载 Token 失败: {}", e.getMessage());
        }
    }

    /**
     * 获取有效的 Access Token（自动刷新）
     */
    public String getAccessToken() {
        // 快速路径：token未过期直接返回
        if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return accessToken;
        }
        // 需要刷新时加锁，避免并发重复刷新
        tokenLock.lock();
        try {
            // double-check：锁内再检查一次，防止多个线程排队后重复刷新
            if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
                return accessToken;
            }
            return doRefreshToken();
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 强制刷新 Token
     */
    public String refreshToken() {
        tokenLock.lock();
        try {
            return doRefreshToken();
        } finally {
            tokenLock.unlock();
        }
    }

    private String doRefreshToken() {
        try {
            String credentials = clientId + ":" + clientSecret;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encoded);

            String url = baseUrl + tokenUrl + "?grant_type=client_credentials&scope=client";
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("正在刷新 IHR360 OAuth Token...");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                String newToken = body.getStr("access_token");
                int expiresIn = body.getInt("expires_in", 7200);
                log.info("IHR360 Token API返回 expires_in={} 秒", expiresIn);
                // 提前5分钟过期，但不低于60秒，避免短有效期token刷新后立即过期
                int effectiveSeconds = Math.max(expiresIn - 300, 60);
                this.tokenExpireTime = LocalDateTime.now().plusSeconds(effectiveSeconds);
                this.accessToken = newToken;

                // 保存到数据库
                saveTokenToDatabase(newToken, expiresIn);

                log.info("IHR360 Token 刷新成功, 有效期至: {}", tokenExpireTime);
                return accessToken;
            } else {
                throw new RuntimeException("刷新 Token 失败, HTTP状态: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("刷新 IHR360 Token 异常: {}", e.getMessage(), e);
            throw new RuntimeException("刷新 Token 失败: " + e.getMessage(), e);
        }
    }

    private void saveTokenToDatabase(String token, int expiresIn) {
        try {
            accessTokenMapper.deleteAll();
            IhrAccessToken tokenEntity = new IhrAccessToken();
            tokenEntity.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
            tokenEntity.setAccessToken(token);
            tokenEntity.setExpiresIn(expiresIn);
            tokenEntity.setCreateTime(new java.util.Date());
            tokenEntity.setUpdateTime(new java.util.Date());
            accessTokenMapper.insertBatch(java.util.Collections.singletonList(tokenEntity));
        } catch (Exception e) {
            log.warn("保存 Token 到数据库失败: {}", e.getMessage());
        }
    }

    /**
     * 构建带 Authorization 头的 HttpEntity
     */
    private HttpEntity<String> buildAuthEntity() {
        return buildAuthEntity(null);
    }

    private HttpEntity<String> buildAuthEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAccessToken());
        return new HttpEntity<>(body, headers);
    }

    /**
     * GET 请求（带重试）
     */
    public String get(String path) {
        return executeWithRetry(() -> {
            String url = baseUrl + path;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildAuthEntity(), String.class);
            return response.getBody();
        });
    }

    /**
     * POST 请求（带重试）
     */
    public String post(String path, String body) {
        return executeWithRetry(() -> {
            String url = baseUrl + path;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, buildAuthEntity(body), String.class);
            return response.getBody();
        });
    }

    private <T> T executeWithRetry(IhrRequestCallback<T> callback) {
        int retryCount = 0;
        while (true) {
            try {
                return callback.execute();
            } catch (RuntimeException e) {
                // 如果是 token 失效的情况，尝试刷新
                if (isTokenExpired(e) && retryCount < MAX_RETRY) {
                    log.warn("Token 可能已过期，尝试刷新...");
                    refreshToken();
                    retryCount++;
                    continue;
                }
                // 网络错误重试
                if ((e instanceof ResourceAccessException) && retryCount < MAX_RETRY) {
                    log.warn("网络异常，等待 {} ms 后重试 (第 {} 次)...", RETRY_DELAY_MS, retryCount + 1);
                    retryCount++;
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isTokenExpired(Exception e) {
        String message = e.getMessage();
        return message != null && (message.contains("401") || message.contains("Unauthorized") ||
                message.contains("invalid_token") || message.contains("expired"));
    }

    /**
     * GET 请求，返回 JSONObject
     */
    public JSONObject getJsonObject(String path) {
        String body = get(path);
        if (body == null || body.isBlank()) {
            throw new RuntimeException("IHR API 返回空响应: " + path);
        }
        return JSONUtil.parseObj(body);
    }

    /**
     * GET 请求，返回 data 字段的 JSONArray
     */
    public JSONArray getDataArray(String path) {
        JSONObject obj = getJsonObject(path);
        return obj.getJSONArray("data");
    }

    /**
     * POST 请求，返回 JSONObject
     */
    public JSONObject postJsonObject(String path, String body) {
        return JSONUtil.parseObj(post(path, body));
    }

    /**
     * POST 请求，返回 data 字段的 JSONArray
     */
    public JSONObject postDataObject(String path, String body) {
        JSONObject obj = postJsonObject(path, body);
        return obj.getJSONObject("data");
    }

    @FunctionalInterface
    private interface IhrRequestCallback<T> {
        T execute() throws Exception;
    }
}
