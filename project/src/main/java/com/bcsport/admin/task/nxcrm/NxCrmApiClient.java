package com.bcsport.admin.task.nxcrm;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nascent.ecrp.opensdk.core.executeClient.ApiClient;
import com.nascent.ecrp.opensdk.core.executeClient.ApiClientImpl;
import com.nascent.ecrp.opensdk.domain.customer.CustomerShopInfo;
import com.nascent.ecrp.opensdk.domain.customer.tag.CustomerTagCategory;
import com.nascent.ecrp.opensdk.domain.customer.tag.TagSetData;
import com.nascent.ecrp.opensdk.request.basis.AccessTokenRegisterRequest;
import com.nascent.ecrp.opensdk.request.customer.ShopByCustomerQueryRequest;
import com.nascent.ecrp.opensdk.domain.trade.TradeDetailVo;
import com.nascent.ecrp.opensdk.request.customer.tag.CustomerTagCategoryQueryRequest;
import com.nascent.ecrp.opensdk.request.customer.tag.MultipleCustomerTagSetRequest;
import com.nascent.ecrp.opensdk.response.basis.AccessTokenRegisterResponse;
import com.nascent.ecrp.opensdk.response.customer.ShopByCustomerQueryResponse;
import com.nascent.ecrp.opensdk.request.trade.TradeSaveRequest;
import com.nascent.ecrp.opensdk.response.trade.TradeSaveResponse;
import com.nascent.ecrp.opensdk.response.customer.tag.CustomerTagCategoryQueryResponse;
import com.nascent.ecrp.opensdk.response.customer.tag.MultipleCustomerTagSetResponse;
import com.nascent.ecrp.opensdk.request.customer.IncrementTagQueryRequest;
import com.nascent.ecrp.opensdk.response.customer.tag.IncrementTagQueryResponse;
import com.nascent.ecrp.opensdk.domain.customer.tag.IncrementTagInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component("nxCrmApiClient")
public class NxCrmApiClient {

    @Value("${nxcrm.app-key}")
    private String appKey;

    @Value("${nxcrm.group-id}")
    private Long groupId;

    @Value("${nxcrm.app-secret}")
    private String appSecret;

    @Value("${nxcrm.server-url}")
    private String serverUrl;

    @Value("${nxcrm.area-id}")
    private Long areaId;

    private volatile String accessToken;
    private volatile LocalDateTime tokenExpireTime;
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        log.info("NxCrmApiClient initialized, serverUrl={}", serverUrl);
    }

    public String getAccessToken() {
        if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return accessToken;
        }
        tokenRefreshLock.lock();
        try {
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
        AccessTokenRegisterRequest request = new AccessTokenRegisterRequest();
        request.setAppKey(appKey);
        request.setGroupId(groupId);
        request.setForceRefresh(true);
        request.setAppSecret(appSecret);
        request.setServerUrl(serverUrl);

        try {
            ApiClient apiClient = new ApiClientImpl(request);
            AccessTokenRegisterResponse response = apiClient.execute(request);
            if (response == null) {
                throw new RuntimeException("南讯CRM Token获取失败: 无响应");
            }
            if (!"200".equals(response.getCode())) {
                throw new RuntimeException("南讯CRM Token获取失败: " + response.getBody());
            }
            JSONObject body = JSONUtil.parseObj(response.getBody());
            JSONObject result = body.getJSONObject("result");
            String token = result != null ? result.getStr("accessToken") : null;
            if (token == null || token.trim().isEmpty()) {
                throw new RuntimeException("南讯CRM Token获取失败: 响应中无accessToken, body=" + response.getBody());
            }
            String expiredTime = result.getStr("expiredTime");
            this.accessToken = token;
            if (expiredTime != null) {
                this.tokenExpireTime = LocalDateTime.parse(expiredTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .minusMinutes(5);
            } else {
                this.tokenExpireTime = LocalDateTime.now().plusHours(23);
            }
            log.info("南讯CRM Token刷新成功, expiredTime={}", expiredTime);
            return this.accessToken;
        } catch (Exception e) {
            log.error("南讯CRM Token刷新失败: {}", e.getMessage(), e);
            throw new RuntimeException("南讯CRM Token刷新失败", e);
        }
    }

    /**
     * 批量给客户打标签
     *
     * @param nasOuidList 会员nasOuid列表
     * @param tagDataList  标签数据列表
     */
    public String multipleCustomerTagSet(List<String> nasOuidList, List<TagSetData> tagDataList) {
        String token = getAccessToken();
        try {
            List<com.nascent.ecrp.opensdk.domain.customer.NickAndPlatform> nickList = new ArrayList<>();
            for (String nasOuid : nasOuidList) {
                com.nascent.ecrp.opensdk.domain.customer.NickAndPlatform nap = new com.nascent.ecrp.opensdk.domain.customer.NickAndPlatform();
                nap.setNasOuid(nasOuid);
                nap.setPlatform(0);
                nickList.add(nap);
            }

            MultipleCustomerTagSetRequest request = new MultipleCustomerTagSetRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setNickAndPlatformInfoList(nickList);
            request.setTagDataList(tagDataList);
            request.setAreaId(areaId);
            request.setShopId(102551340L);
            request.setOutShopId("BCHY");

            log.info("南讯CRM批量打标签入参: nickAndPlatformInfoList={}, tagDataList={}, areaId={}, outShopId={}, shopId={}",
                nickList, tagDataList, areaId, "BCHY", 102551340L);

            ApiClient apiClient = new ApiClientImpl(request);
            MultipleCustomerTagSetResponse response = apiClient.execute(request);

            if (response == null || !response.success) {
                throw new RuntimeException("南讯CRM批量打标签失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }
            log.info("南讯CRM批量打标签成功: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("南讯CRM批量打标签失败: {}", e.getMessage(), e);
            throw new RuntimeException("南讯CRM批量打标签失败", e);
        }
    }

    /**
     * 通过nasOuid查询会员所属店铺信息
     */
    public List<CustomerShopInfo> getShopByCustomer(String nasOuid) {
        String token = getAccessToken();
        try {
            ShopByCustomerQueryRequest request = new ShopByCustomerQueryRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setNasOuid(nasOuid);


            ApiClient apiClient = new ApiClientImpl(request);
            ShopByCustomerQueryResponse response = apiClient.execute(request);

            if (response == null || !response.success) {
                throw new RuntimeException("查询会员店铺失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }
            return response.getResult();
        } catch (Exception e) {
            log.error("查询会员店铺失败, nasOuid={}: {}", nasOuid, e.getMessage(), e);
            throw new RuntimeException("查询会员店铺失败", e);
        }
    }

    public List<CustomerTagCategory> getTagCategories() {
        String token = getAccessToken();
        log.info("查询标签分类, accessToken={}", token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "NULL");
        try {
            CustomerTagCategoryQueryRequest request = new CustomerTagCategoryQueryRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);

            ApiClient apiClient = new ApiClientImpl(request);
            CustomerTagCategoryQueryResponse response = apiClient.execute(request);

            if (response == null || !response.success) {
                throw new RuntimeException("查询标签分类失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }
            log.info("查询标签分类成功, 共{}条", response.getResult() != null ? response.getResult().size() : 0);
            return response.getResult();
        } catch (Exception e) {
            log.error("查询标签分类失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询标签分类失败", e);
        }
    }

    public List<IncrementTagInfo> getIncrementTags(Integer entityCode, Date startTime, Date endTime) {
        String token = getAccessToken();
        try {
            IncrementTagQueryRequest request = new IncrementTagQueryRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setEntityCode(entityCode != null ? entityCode : 1);
            if (startTime != null) request.setStartTime(startTime);
            if (endTime != null) request.setEndTime(endTime);

            ApiClient apiClient = new ApiClientImpl(request);
            IncrementTagQueryResponse response = apiClient.execute(request);

            if (response == null || !response.success) {
                throw new RuntimeException("查询增量标签失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }
            log.info("查询增量标签成功, 共{}条", response.getResult() != null ? response.getResult().size() : 0);
            return response.getResult();
        } catch (Exception e) {
            log.error("查询增量标签失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询增量标签失败", e);
        }
    }

    public void saveOrders(List<TradeDetailVo> orders, String shopId) {
        String token = getAccessToken();
        try {
            TradeSaveRequest request = new TradeSaveRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setTradeDetailVoList(orders);
            request.setOutShopId(shopId);
            request.setIsHistoryData(true);

            ApiClient apiClient = new ApiClientImpl(request);
            TradeSaveResponse response = apiClient.execute(request);

            if (response == null || !response.success) {
                throw new RuntimeException("订单同步失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }
            log.info("订单同步成功, shopId={}, 批次{}条", shopId, orders.size());
        } catch (Exception e) {
            log.error("订单同步失败, shopId={}: {}", shopId, e.getMessage(), e);
            throw new RuntimeException("订单同步失败", e);
        }
    }
}
