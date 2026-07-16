package com.bcsport.admin.task.erp;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 伯俊ERP接口客户端
 * <p>
 * 协议规范：
 * <ul>
 *   <li>HTTP POST, Content-Type: application/x-www-form-urlencoded</li>
 *   <li>签名: sip_sign = MD5(sip_appkey + sip_timestamp + MD5(appSecret))</li>
 *   <li>请求体: sip_appkey=xxx&amp;sip_timestamp=xxx&amp;sip_sign=xxx&amp;transactions=xxx</li>
 *   <li>transactions: JSONArray, 每项包含 {id, command, params: {table, ...}}</li>
 * </ul>
 */
@Slf4j
@Component
public class BjErpApiClient {

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Value("${bjerp.api-url}")
    private String apiUrl;

    @Value("${bjerp.app-key}")
    private String appKey;

    @Value("${bjerp.app-secret}")
    private String appSecret;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
        log.info("BjErpApiClient initialized, apiUrl={}", apiUrl);
    }

    // ==================== 核心调用 ====================

    /**
     * 调用伯俊ERP接口
     *
     * @param transactions 业务数据JSONArray，每项格式: {id, command, params}
     * @return 原始响应JSONArray，格式: [{code, id, message, objectid}]
     */
    public JSONArray call(JSONArray transactions) {
        return doCall(transactions, 0);
    }

    private JSONArray doCall(JSONArray transactions, int retryCount) {
        try {
            // 1. 时间戳
            String timestamp = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");

            // 2. 签名: MD5(sip_appkey + sip_timestamp + MD5(appSecret))
            String secretMd5 = DigestUtil.md5Hex(appSecret);
            String sign = DigestUtil.md5Hex(appKey + timestamp + secretMd5);

            // 3. 拼接请求体并URL编码
            String body = "sip_appkey=" + encode(appKey)
                    + "&sip_timestamp=" + encode(timestamp)
                    + "&sip_sign=" + encode(sign)
                    + "&transactions=" + encode(transactions.toString());

            // 4. 发送
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            log.debug("伯俊ERP请求: {}, transactions.size={}", apiUrl, transactions.size());

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            // 5. 解析响应（伯俊返回JSONArray格式）
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String respBody = response.getBody().trim();
                JSONArray result;
                if (respBody.startsWith("[")) {
                    result = JSONUtil.parseArray(respBody);
                } else {
                    // 兼容非数组响应
                    result = new JSONArray();
                    result.add(JSONUtil.parseObj(respBody));
                }

                // 检查业务错误并记录日志
                if (!isSuccess(result)) {
                    String errMsg = extractErrorMessage(result);
                    log.error("伯俊ERP业务错误: apiUrl={}, transactions={}, response={}", apiUrl, transactions, result);
                    throw new RuntimeException("伯俊ERP业务错误: " + errMsg);
                }

                log.debug("伯俊ERP响应: {}", result);
                return result;
            }
            log.error("伯俊ERP返回非200: apiUrl={}, statusCode={}", apiUrl, response.getStatusCode());
            throw new RuntimeException("伯俊ERP返回非200: " + response.getStatusCode());

        } catch (ResourceAccessException e) {
            if (retryCount < MAX_RETRY) {
                log.warn("伯俊ERP超时，第{}次重试", retryCount + 1);
                sleep(RETRY_DELAY_MS);
                return doCall(transactions, retryCount + 1);
            }
            throw new RuntimeException("伯俊ERP调用失败(已重试" + MAX_RETRY + "次): " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("伯俊ERP调用异常: " + e.getMessage(), e);
        }
    }

    // ==================== 响应解析工具 ====================

    /**
     * 检查伯俊响应是否全部成功
     *
     * @param response 伯俊返回的JSONArray
     * @return true=全部成功(code=0)
     */
    public static boolean isSuccess(JSONArray response) {
        if (response == null || response.isEmpty()) return false;
        for (int i = 0; i < response.size(); i++) {
            JSONObject item = response.getJSONObject(i);
            if (item.getInt("code", -1) != 0) return false;
        }
        return true;
    }

    /**
     * 从伯俊响应中提取第一条的objectid（ERP记录ID）
     *
     * @param response 伯俊返回的JSONArray
     * @return objectid，失败时返回null
     */
    public static Long extractObjectId(JSONArray response) {
        if (response == null || response.isEmpty()) return null;
        return response.getJSONObject(0).getLong("objectid");
    }

    /**
     * 从伯俊响应中提取错误信息
     *
     * @param response 伯俊返回的JSONArray
     * @return 错误信息，成功时返回null
     */
    public static String extractErrorMessage(JSONArray response) {
        if (response == null || response.isEmpty()) return "响应为空";
        for (int i = 0; i < response.size(); i++) {
            JSONObject item = response.getJSONObject(i);
            if (item.getInt("code", -1) != 0) {
                return item.getStr("message", "未知错误(code=" + item.getInt("code") + ")");
            }
        }
        return null;
    }

    /**
     * 判断错误信息是否为"数据已存在"（如"输入的数据已存在:编号"）。
     * 用于入职同步时把"已存在"识别为已跳过而非失败。
     *
     * @param errMsg 错误信息
     * @return true 表示是"已存在"类错误
     */
    public static boolean isAlreadyExists(String errMsg) {
        return errMsg != null && errMsg.contains("已存在");
    }

    // ==================== 工具 ====================

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
