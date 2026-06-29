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

import jakarta.annotation.PostConstruct;
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

    //自建应用 帆软FR
    @Value("${qywx.corp-secret}")
    private String corpSecret;

    @Value("${qywx.contacts-secret:}")
    private String contactsSecret;

    @Value("${qywx.api-base-url:https://qyapi.weixin.qq.com}")
    private String apiBaseUrl;

    private RestTemplate restTemplate;

    private volatile String accessToken;
    private volatile LocalDateTime tokenExpireTime;

    private volatile String contactsAccessToken;
    private volatile LocalDateTime contactsTokenExpireTime;

    @Autowired
    private QywxAccessTokenMapper accessTokenMapper;

    // 锁用于保护token刷新，避免多线程同时刷新
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();
    private final ReentrantLock contactsTokenRefreshLock = new ReentrantLock();

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
        }, () -> {});
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

    /**
     * 获取通讯录管理 access_token（使用独立的 contacts-secret）
     */
    public String getContactsAccessToken() {
        if (contactsSecret == null || contactsSecret.isEmpty()) {
            return getAccessToken();
        }

        if (contactsAccessToken != null && contactsTokenExpireTime != null && LocalDateTime.now().isBefore(contactsTokenExpireTime)) {
            return contactsAccessToken;
        }

        contactsTokenRefreshLock.lock();
        try {
            if (contactsAccessToken != null && contactsTokenExpireTime != null && LocalDateTime.now().isBefore(contactsTokenExpireTime)) {
                return contactsAccessToken;
            }
            return doRefreshContactsToken();
        } finally {
            contactsTokenRefreshLock.unlock();
        }
    }

    public String refreshContactsToken() {
        if (contactsSecret == null || contactsSecret.isEmpty()) {
            return refreshToken();
        }
        contactsTokenRefreshLock.lock();
        try {
            return doRefreshContactsToken();
        } finally {
            contactsTokenRefreshLock.unlock();
        }
    }

    private String doRefreshContactsToken() {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + contactsSecret;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            log.info("Refreshing QYWX contacts access token...");
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get contacts token, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }

                String newToken = body.getStr("access_token");
                Integer expiresIn = body.getInt("expires_in", 7200);
                this.contactsTokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn - 300);
                this.contactsAccessToken = newToken;

                log.info("QYWX contacts token refreshed successfully");
                return contactsAccessToken;
            } else {
                throw new RuntimeException("Failed to refresh contacts token");
            }
        }, () -> {});
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
        return executeWithRetry(callback, this::refreshToken);
    }

    private <T> T executeWithRetry(QywxApiCallback<T> callback, Runnable tokenRefresher) {
        int retryCount = 0;
        while (true) {
            try {
                return callback.execute();
            } catch (RuntimeException e) {
                if (isTokenExpiredException(e) && retryCount < MAX_RETRY) {
                    log.warn("Token may be expired, refreshing... (retry {}/{})", retryCount + 1, MAX_RETRY);
                    tokenRefresher.run();
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

            log.info("API: 获取部门列表...");
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

                log.info("API: 获取到 {} 个部门", result.size());
                return result;
            } else {
                throw new RuntimeException("Failed to get department list");
            }
        });
    }

    public List<QywxDepartmentMember> getDepartmentMemberList() {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/simplelist?access_token=" + getAccessToken() + "&department_id=1&fetch_child=1";

            log.info("API: 获取部门成员列表...");
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

                log.info("API: 获取到 {} 个部门成员", result.size());
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

            log.info("API: 获取客户联系成员列表...");
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

                log.info("API: 获取到 {} 个客户联系成员", result.size());
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

            log.info("API: 获取 {} 个用户的群聊列表", userIds.size());
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

            log.info("API: 获取群发消息列表, {} ~ {}", startTime, endTime);
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

            log.info("API: 获取朋友圈列表, {} ~ {}", startTime, endTime);
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

    /**
     * 根据手机号获取用户ID
     */
    public String getUserIdByMobile(String mobile) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/getuserid?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("mobile", mobile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    // 60111/46004表示用户不存在，返回null而不是抛出异常
                    if (errcode == 60111 || errcode == 46004) {
                        return null;
                    }
                    throw new RuntimeException("Failed to get userid by mobile, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body.getStr("userid");
            } else {
                throw new RuntimeException("Failed to get userid by mobile");
            }
        });
    }

    /**
     * 创建用户
     */
    public JSONObject createUser(JSONObject userInfo) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/create?access_token=" + getContactsAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(userInfo.toString(), headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0 && errcode != 60102) {
                    throw new RuntimeException("Failed to create user, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to create user");
            }
        }, this::refreshContactsToken);
    }

    /**
     * 删除用户
     */
    public JSONObject deleteUser(String userid) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/delete?access_token=" + getContactsAccessToken() + "&userid=" + userid;

            String responseBody = doGet(url);

            if (responseBody != null) {
                JSONObject body = JSONUtil.parseObj(responseBody);
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0 && errcode != 60111) {
                    throw new RuntimeException("Failed to delete user " + userid + ", errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to delete user");
            }
        }, this::refreshContactsToken);
    }

    /**
     * 更新用户
     */
    public JSONObject updateUser(JSONObject userInfo) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/user/update?access_token=" + getContactsAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(userInfo.toString(), headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to update user, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to update user");
            }
        }, this::refreshContactsToken);
    }

    // ==================== 标签管理 API ====================

    /**
     * 获取企业标签库
     */
    public JSONObject getCorpTagList(String tagId, String groupId) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/get_corp_tag_list?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            if (tagId != null || groupId != null) {
                JSONArray tagIdArr = tagId != null ? new JSONArray().put(tagId) : null;
                JSONArray groupIdArr = groupId != null ? new JSONArray().put(groupId) : null;
                if (tagIdArr != null) requestBody.set("tag_id", tagIdArr);
                if (groupIdArr != null) requestBody.set("group_id", groupIdArr);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("API: 获取企业标签库...");
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to get corp tag list, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to get corp tag list");
            }
        });
    }

    /**
     * 添加企业客户标签
     */
    public JSONObject addCorpTag(String groupId, String groupName, List<String> tagNames) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/add_corp_tag?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            if (groupId != null) {
                requestBody.set("group_id", groupId);
            }
            if (groupName != null) {
                requestBody.set("group_name", groupName);
            }

            JSONArray tagArr = new JSONArray();
            for (String name : tagNames) {
                JSONObject tag = new JSONObject();
                tag.set("name", name);
                tagArr.add(tag);
            }
            requestBody.set("tag", tagArr);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Adding corp tag, group: {}, tags: {}", groupName, tagNames);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to add corp tag, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to add corp tag");
            }
        });
    }

    /**
     * 编辑企业客户标签
     */
    public JSONObject editCorpTag(String id, String name, Integer order) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/edit_corp_tag?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("id", id);
            if (name != null) requestBody.set("name", name);
            if (order != null) requestBody.set("order", order);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to edit corp tag, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to edit corp tag");
            }
        });
    }

    /**
     * 删除企业客户标签
     */
    public JSONObject delCorpTag(List<String> tagIds, List<String> groupIds) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/del_corp_tag?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            if (tagIds != null && !tagIds.isEmpty()) {
                requestBody.set("tag_id", new JSONArray(tagIds));
            }
            if (groupIds != null && !groupIds.isEmpty()) {
                requestBody.set("group_id", new JSONArray(groupIds));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            log.info("Deleting corp tags: {}", tagIds);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    throw new RuntimeException("Failed to del corp tag, errcode: " + errcode + ", errmsg: " + body.getStr("errmsg"));
                }
                return body;
            } else {
                throw new RuntimeException("Failed to del corp tag");
            }
        });
    }

    /**
     * 给客户打标/移除标签
     * markTag API 要求: userid + externalUserid + addTag/removeTag
     * 每次最多添加/移除50个标签
     */
    public JSONObject markTag(String userid, String externalUserid,
                              List<String> addTag, List<String> removeTag) {
        return executeWithRetry(() -> {
            String url = apiBaseUrl + "/cgi-bin/externalcontact/mark_tag?access_token=" + getAccessToken();

            JSONObject requestBody = new JSONObject();
            requestBody.set("userid", userid);
            requestBody.set("external_userid", externalUserid);
            if (addTag != null && !addTag.isEmpty()) {
                requestBody.set("add_tag", new JSONArray(addTag));
            }
            if (removeTag != null && !removeTag.isEmpty()) {
                requestBody.set("remove_tag", new JSONArray(removeTag));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.OK && response.getBody() != null) {
                JSONObject body = JSONUtil.parseObj(response.getBody());
                Integer errcode = body.getInt("errcode");
                if (errcode != null && errcode != 0) {
                    log.warn("markTag failed for externalUserid: {}, errcode: {}, errmsg: {}",
                            externalUserid, errcode, body.getStr("errmsg"));
                    return body;
                }
                return body;
            } else {
                throw new RuntimeException("Failed to mark tag");
            }
        });
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    /**
     * 校验邮箱格式，null / "null" / 空白 / 格式不符 均返回 false
     */
    public boolean isValidEmail(String email) {
        if (email == null || "null".equals(email) || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @FunctionalInterface
    private interface QywxApiCallback<T> {
        T execute() throws Exception;
    }
}
