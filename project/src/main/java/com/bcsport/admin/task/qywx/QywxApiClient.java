package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bcsport.admin.entity.qywx.QywxAccessToken;
import com.bcsport.admin.entity.qywx.QywxDepartment;
import com.bcsport.admin.entity.qywx.QywxDepartmentMember;
import com.bcsport.admin.qywxmapper.QywxAccessTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 企业微信API客户端
 */
@Slf4j
@Component
public class QywxApiClient {

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Value("${qywx.corp-id}")
    private String corpId;

    @Value("${qywx.corp-secret}")
    private String corpSecret;

    @Value("${qywx.api-base-url:https://qyapi.weixin.qq.com}")
    private String apiBaseUrl;

    private RestTemplate restTemplate;

    private volatile String accessToken;
    private volatile LocalDateTime tokenExpireTime;

    @Autowired
    private QywxAccessTokenMapper accessTokenMapper;

    // 锁用于保护token刷新，避免多线程同时刷新
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        restTemplate.setRequestFactory(factory);
        log.info("QywxApiClient initialized");

        loadTokenFromDatabase();
    }

    private void loadTokenFromDatabase() {
        try {
            String token = accessTokenMapper.queryLatestToken();
            if (token != null) {
                this.accessToken = token;
                this.tokenExpireTime = LocalDateTime.now().plusHours(1);
                log.info("Token loaded from database");
            }
        } catch (Exception e) {
            log.warn("Failed to load token from database: {}", e.getMessage());
        }
    }

    public String getAccessToken() {
        // 先不加锁检查一次，避免不必要的锁竞争
        if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return accessToken;
        }

        // 加锁检查并刷新
        tokenRefreshLock.lock();
        try {
            // 再次检查，防止在等待锁的过程中已经被其他线程刷新了
            if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
                return accessToken;
            }
            return doRefreshToken();
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    public String refreshToken() {
        tokenRefreshLock.lock();
        try {
            return doRefreshToken();
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    private String doRefreshToken() {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + corpSecret;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            log.info("Refreshing QYWX access token...");
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get token, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }

                String newToken = body.getStr("access_token");
                Integer expiresIn = body.getInt("expires_in", 7200);
                this.tokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn - 300);
                this.accessToken = newToken;

                saveTokenToDatabase(newToken, expiresIn);

                log.info("QYWX token refreshed successfully");
                return accessToken;
            } else {
                throw new RuntimeException("Failed to refresh token");
            }
        });
    }

    private void saveTokenToDatabase(String token, Integer expiresIn) {
        try {
            accessTokenMapper.deleteAll();
            QywxAccessToken tokenEntity = new QywxAccessToken();
            tokenEntity.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
            tokenEntity.setAccessToken(token);
            accessTokenMapper.insertBatch(Collections.singletonList(tokenEntity));
        } catch (Exception e) {
            log.warn("Failed to save token to database: {}", e.getMessage());
        }
    }

    private boolean isTokenExpiredException(Exception e) {
        String message = e.getMessage();
        return message != null && (message.contains("40014") || message.contains("42001") ||
                message.contains("expired") || message.contains("invalid"));
    }

    private String doGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private <T> T executeWithRetry(QywxApiCallback<T> callback) {
        int retryCount = 0;
        while (true) {
            try {
                return callback.execute();
            } catch (RuntimeException e) {
                if (isTokenExpiredException(e) && retryCount < MAX_RETRY) {
                    log.warn("Token may be expired, refreshing... (retry {}/{})", retryCount + 1, MAX_RETRY);
                    refreshToken();
                    retryCount++;
                    continue;
                }
                if (e instanceof ResourceAccessException && retryCount < MAX_RETRY) {
                    log.warn("Network error, waiting {} ms before retry (retry {}/{})...", RETRY_DELAY_MS, retryCount + 1, MAX_RETRY);
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

    public List<QywxDepartment> getDepartmentList() {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/department/list?access_token=" + getAccessToken();

            log.info("Fetching QYWX department list...");
            String responseBody = doGet(url);

            if (responseBody != null) {
                JSONObject body = JSONUtil.parseObj(responseBody);
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get department list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }

                JSONArray deptArray = body.getJSONArray("department");
                List<QywxDepartment> result = new ArrayList<>();
                if (deptArray != null) {
                    for (int i = 0; i < deptArray.size(); i++) {
                        JSONObject deptObj = deptArray.getJSONObject(i);
                        QywxDepartment dept = new QywxDepartment();
                        dept.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                        dept.setDepartId(deptObj.getStr("id"));
                        dept.setName(deptObj.getStr("name"));
                        dept.setNameEn(deptObj.getStr("name_en"));
                        dept.setParentId(deptObj.getStr("parentid"));
                        dept.setParentOrder(deptObj.getStr("order"));
                        result.add(dept);
                    }
                }

                log.info("Fetched {} QYWX departments", result.size());
                return result;
            } else {
                throw new RuntimeException("Failed to get department list");
            }
        });
    }

    public List<QywxDepartmentMember> getDepartmentMemberList() {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/simplelist?access_token=" + getAccessToken() + "&department_id=1&fetch_child=1";

            log.info("Fetching QYWX department member list...");
            String responseBody = doGet(url);

            if (responseBody != null) {
                JSONObject body = JSONUtil.parseObj(responseBody);
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get department member list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }

                JSONArray userArray = body.getJSONArray("userlist");
                List<QywxDepartmentMember> result = new ArrayList<>();
                if (userArray != null) {
                    for (int i = 0; i < userArray.size(); i++) {
                        JSONObject userObj = userArray.getJSONObject(i);
                        QywxDepartmentMember member = new QywxDepartmentMember();
                        member.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                        member.setUserid(userObj.getStr("userid"));
                        member.setName(userObj.getStr("name"));
                        member.setOpenUserid(userObj.getStr("open_userid", ""));
                        JSONArray deptArray = userObj.getJSONArray("department");
                        if (deptArray != null && deptArray.size() > 0) {
                            member.setDepartment(deptArray.getInt(0));
                        }
                        result.add(member);
                    }
                }

                log.info("Fetched {} QYWX department members", result.size());
                return result;
            } else {
                throw new RuntimeException("Failed to get department member list");
            }
        });
    }

    /**
     * 获取配置了客户联系功能的成员列表
     */
    public List<String> getFollowUserList() {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/get_follow_user_list?access_token=" + getAccessToken();

            log.info("Fetching QYWX follow user list...");
            String responseBody = doGet(url);

            if (responseBody != null) {
                JSONObject body = JSONUtil.parseObj(responseBody);
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get follow user list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }

                JSONArray userArray = body.getJSONArray("follow_user");
                List<String> result = new ArrayList<>();
                if (userArray != null) {
                    for (int i = 0; i < userArray.size(); i++) {
                        result.add(userArray.getStr(i));
                    }
                }

                log.info("Fetched {} QYWX follow users", result.size());
                return result;
            } else {
                throw new RuntimeException("Failed to get follow user list");
            }
        });
    }

    /**
     * 获取用户详情
     * 注意：如果返回错误，会记录日志但返回错误对象而不是抛出异常
     */
    public JSONObject getUserDetail(String userid) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/get?access_token=" + getAccessToken() + "&userid=" + userid;

            String responseBody = doGet(url);

            if (responseBody != null) {
                JSONObject body = JSONUtil.parseObj(responseBody);
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    log.warn("API returned error for userid: {}, errcode: {}, errmsg: {}",
                            userid, errcode, body.getStr("errmsg"));
                    // 返回包含错误码的对象，让调用方处理
                    JSONObject errorObj = new JSONObject();
                    errorObj.set("userid", userid);
                    errorObj.set("errcode", errcode);
                    errorObj.set("errmsg", body.getStr("errmsg"));
                    return errorObj;
                }
                return body;
            } else {
                log.warn("Empty response for userid: {}", userid);
                JSONObject errorObj = new JSONObject();
                errorObj.set("userid", userid);
                errorObj.set("errcode", -1);
                errorObj.set("errmsg", "Empty response");
                return errorObj;
            }
        });
    }

    /**
     * 获取所有部门成员（别名方法，功能同getDepartmentMemberList）
     */
    public List<QywxDepartmentMember> getDepartmentListAll() {
        return getDepartmentMemberList();
    }

    /**
     * 批量获取客户详情
     */
    public JSONObject batchGetByUser(List<String> userIds, String cursor) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/batch/get_by_user?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("userid_list", userIds);
            requestBody.set("cursor", cursor != null ? cursor : "");
            requestBody.set("limit", 100);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Fetching customer details for {} users...", userIds.size());
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to batch get customer details, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to batch get customer details");
            }
        });
    }

    /**
     * 批量获取成员的客户群列表（最多支持100个成员）
     */
    public JSONObject getGroupChatList(List<String> userIds, String cursor) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/groupchat/list?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("status_filter", 0);
            JSONObject ownerFilter = new JSONObject();
            JSONArray userIdList = new JSONArray();
            for (String userId : userIds) {
                userIdList.add(userId);
            }
            ownerFilter.set("userid_list", userIdList);
            requestBody.set("owner_filter", ownerFilter);
            requestBody.set("cursor", cursor != null ? cursor : "");
            requestBody.set("limit", 1000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Fetching group chat list for {} users", userIds.size());
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get group chat list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to get group chat list");
            }
        });
    }

    /**
     * 获取客户群详情
     */
    public JSONObject getGroupChatDetail(String chatId) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/groupchat/get?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("chat_id", chatId);
            requestBody.set("need_name", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Fetching group chat detail for chatId: {}", chatId);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    log.warn("Failed to get group chat detail for chatId: {}, errcode: {}, errmsg: {}",
                            chatId, errcode, body.getStr("errmsg"));
                    return body;
                }
                return body;
            } else {
                throw new RuntimeException("Failed to get group chat detail");
            }
        });
    }

    /**
     * 获取群聊统计数据
     */
    public JSONObject getGroupChatStatistic(List<String> userIds, long dayBeginTime, long dayEndTime) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/groupchat/statistic?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("day_begin_time", dayBeginTime);
            requestBody.set("day_end_time", dayEndTime);
            JSONObject ownerFilter = new JSONObject();
            JSONArray userIdList = new JSONArray();
            for (String userId : userIds) {
                userIdList.add(userId);
            }
            ownerFilter.set("userid_list", userIdList);
            requestBody.set("owner_filter", ownerFilter);
            requestBody.set("order_by", 2);
            requestBody.set("order_asc", 0);
            requestBody.set("offset", 0);
            requestBody.set("limit", 1000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Fetching group chat statistic for {} users", userIds.size());
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get group chat statistic, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to get group chat statistic");
            }
        });
    }

    /**
     * 获取群发消息记录列表
     */
    public JSONObject getMassMessageList(long startTime, long endTime, String cursor, String chatType) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/get_groupmsg_list_v2?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("chat_type", chatType);
            requestBody.set("start_time", startTime);
            requestBody.set("end_time", endTime);
            requestBody.set("cursor", cursor != null ? cursor : "");
            requestBody.set("limit", 100);
            requestBody.set("filter_type", "1");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Fetching mass message list from {} to {}", startTime, endTime);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get mass message list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to get mass message list");
            }
        });
    }

    /**
     * 获取朋友圈列表
     */
    public JSONObject getMomentList(long startTime, long endTime, String cursor) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/get_moment_list?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("start_time", startTime);
            requestBody.set("end_time", endTime);
            requestBody.set("filter_type", 2);
            requestBody.set("cursor", cursor != null ? cursor : "");
            requestBody.set("limit", 100);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Fetching moment list from {} to {}", startTime, endTime);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get moment list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to get moment list");
            }
        });
    }

    @FunctionalInterface
    private interface QywxApiCallback<T> {
        T execute() throws Exception;
    }
}
