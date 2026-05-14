package com.bcsport.admin.task.ydkl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.bcsport.admin.dto.ydkl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 云盯(YD) API 统一客户端
 */
@Slf4j
@Component("ydApiClient")
public class YdApiClient {

    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Value("${ydkl.account}")
    private String account;

    @Value("${ydkl.enterprise-code}")
    private String enterpriseCode;

    @Value("${ydkl.password}")
    private String password;

    @Value("${ydkl.token-url}")
    private String tokenUrl;

    @Value("${ydkl.shop-list-url}")
    private String shopListUrl;

    @Value("${ydkl.customer-flow-url}")
    private String customerFlowUrl;

    @Value("${ydkl.weather-url}")
    private String weatherUrl;

    /**
     * 获取云盯 Token
     */
    public YdTokenResponse getToken() {
        YdTokenRequest request = new YdTokenRequest();
        request.setAccount(account);
        request.setEnterpriseCode(enterpriseCode);
        request.setPassword(password);

        return executeWithRetry("获取云盯Token", () -> {
            try (HttpResponse response = HttpRequest.post(tokenUrl)
                    .header("Content-Type", "application/json")
                    .header("api-version", "v1")
                    .body(JSONUtil.toJsonStr(request))
                    .timeout(CONNECT_TIMEOUT_MS)
                    .setReadTimeout(READ_TIMEOUT_MS)
                    .execute()) {
                String body = response.body();
                if (response.isOk() && StrUtil.isNotBlank(body)) {
                    return JSONUtil.toBean(body, YdTokenResponse.class);
                }
                throw new RuntimeException("获取云盯Token失败, HTTP状态: " + response.getStatus());
            }
        }, null);
    }

    /**
     * 获取安装客流统计的门店列表
     */
    public List<YdShopInfo> getShopList(YdTokenResponse token) {
        return executeWithRetry("获取门店列表", () -> {
            try (HttpResponse response = HttpRequest.get(shopListUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", token.getTokenType() + " " + token.getAccessToken())
                    .header("api-version", "v1")
                    .timeout(CONNECT_TIMEOUT_MS)
                    .setReadTimeout(READ_TIMEOUT_MS)
                    .execute()) {
                String body = response.body();
                if (response.isOk() && StrUtil.isNotBlank(body)) {
                    return JSONUtil.toList(body, YdShopInfo.class);
                }
                throw new RuntimeException("获取门店列表失败, HTTP状态: " + response.getStatus());
            }
        }, new ArrayList<>());
    }

    /**
     * 获取门店客流数据
     */
    public YdCustomerFlowResponse getCustomerFlowData(YdTokenResponse token,
                                                       YdCustomerFlowRequest request,
                                                       int page, int size) {
        String url = customerFlowUrl + "?page=" + page + "&size=" + size;
        return executeWithRetry("获取客流数据", () -> {
            try (HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", token.getTokenType() + " " + token.getAccessToken())
                    .header("api-version", "v2")
                    .body(JSONUtil.toJsonStr(request))
                    .timeout(CONNECT_TIMEOUT_MS)
                    .setReadTimeout(READ_TIMEOUT_MS)
                    .execute()) {
                String body = response.body();
                log.debug("客流数据响应: {}", body);
                if (response.isOk() && StrUtil.isNotBlank(body)) {
                    return JSONUtil.toBean(body, YdCustomerFlowResponse.class);
                }
                throw new RuntimeException("获取客流数据失败, HTTP状态: " + response.getStatus());
            }
        }, null);
    }

    /**
     * 获取天气数据
     */
    public YdWeatherResponse getWeatherData(YdTokenResponse token,
                                             YdWeatherRequest request,
                                             int page, int size) {
        String url = weatherUrl + "?page=" + page + "&size=" + size;
        return executeWithRetry("获取天气数据", () -> {
            try (HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", token.getTokenType() + " " + token.getAccessToken())
                    .header("api-version", "v1")
                    .body(JSONUtil.toJsonStr(request))
                    .timeout(CONNECT_TIMEOUT_MS)
                    .setReadTimeout(READ_TIMEOUT_MS)
                    .execute()) {
                String body = response.body();
                log.debug("天气数据响应: {}", body);
                if (response.isOk() && StrUtil.isNotBlank(body)) {
                    return JSONUtil.toBean(body, YdWeatherResponse.class);
                }
                throw new RuntimeException("获取天气数据失败, HTTP状态: " + response.getStatus());
            }
        }, null);
    }

    /**
     * 构建门店ID列表
     */
    public List<String> extractShopIds(List<YdShopInfo> shops) {
        List<String> ids = new ArrayList<>();
        for (YdShopInfo shop : shops) {
            ids.add(shop.getStoreId());
        }
        return ids;
    }

    @FunctionalInterface
    private interface YdApiCallback<T> {
        T execute() throws Exception;
    }

    private <T> T executeWithRetry(String action, YdApiCallback<T> callback, T fallback) {
        int retryCount = 0;
        while (true) {
            try {
                return callback.execute();
            } catch (Exception e) {
                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    log.warn("{}失败, 等待{}ms后重试(第{}次): {}", action, RETRY_DELAY_MS, retryCount, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                log.error("{}失败, 已重试{}次: {}", action, MAX_RETRY, e.getMessage(), e);
                break;
            }
        }
        return fallback;
    }
}
