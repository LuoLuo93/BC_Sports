package com.bcsport.admin.task.nxcrm;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nascent.ecrp.opensdk.core.executeClient.ApiClient;
import com.nascent.ecrp.opensdk.core.executeClient.ApiClientImpl;
import com.nascent.ecrp.opensdk.domain.customer.CustomerShopInfo;
import com.nascent.ecrp.opensdk.domain.customer.tag.CustomerTagCategory;
import com.nascent.ecrp.opensdk.domain.customer.tag.TagSetData;
import com.nascent.ecrp.opensdk.request.basis.AccessTokenRegisterRequest;
import com.nascent.ecrp.opensdk.domain.customer.CustomerGradeUpdateInfo;
import com.nascent.ecrp.opensdk.domain.customer.CustomerSaveInfo;
import com.nascent.ecrp.opensdk.request.customer.BatchCustomerSaveRequest;
import com.nascent.ecrp.opensdk.request.customer.CustomerGradeUpdateRequest;
import com.nascent.ecrp.opensdk.request.customer.CustomerUnbindRequest;
import com.nascent.ecrp.opensdk.request.customer.ShopByCustomerQueryRequest;
import com.nascent.ecrp.opensdk.domain.trade.TradeDetailVo;
import com.nascent.ecrp.opensdk.request.customer.tag.CustomerTagCategoryQueryRequest;
import com.nascent.ecrp.opensdk.request.customer.tag.MultipleCustomerTagSetRequest;
import com.nascent.ecrp.opensdk.response.basis.AccessTokenRegisterResponse;
import com.nascent.ecrp.opensdk.response.customer.ShopByCustomerQueryResponse;
import com.nascent.ecrp.opensdk.response.customer.BatchCustomerSaveResponse;
import com.nascent.ecrp.opensdk.response.customer.CustomerGradeUpdateResponse;
import com.nascent.ecrp.opensdk.response.customer.CustomerUnbindResponse;
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

            if (response == null || !Boolean.TRUE.equals(response.success)) {
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

            if (response == null || !Boolean.TRUE.equals(response.success)) {
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

            if (response == null || !Boolean.TRUE.equals(response.success)) {
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

            if (response == null || !Boolean.TRUE.equals(response.success)) {
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

    /**
     * 南讯会员解绑（单条）
     * <p>对应南讯接口：{@code /openApi/customer/customerUnbind}
     *
     * @param nasOuid 南讯会员 ID
     * @param shopId  外部门店 ID（outShopId）
     * @return 解析后的响应结果
     */
    /**
     * 南讯会员解绑（单条）
     * <p>对应南讯接口：{@code /openApi/customer/customerUnbind}
     *
     * <p>返回值解析规则：
     * <ul>
     *   <li>SDK 抛异常（网络超时等）→ throw，由调用方重试</li>
     *   <li>response == null → throw，由调用方重试</li>
     *   <li>success=false（业务失败，如"会员不存在"）→ 正常返回 UnbindResponse（不重试）</li>
     *   <li>success=true → 正常返回 UnbindResponse</li>
     * </ul>
     *
     * @param nasOuid 南讯会员 ID
     * @param shopId  外部门店 ID（outShopId）
     * @return 解析后的响应结果（含 code/msg/result，调用方通过 response.success 判断业务成败）
     * @throws RuntimeException 仅在系统异常时抛出（网络、SDK 错误、无响应）
     */
    public UnbindResponse unbindCustomer(String nasOuid, String shopId) {
        String token = getAccessToken();
        try {
            CustomerUnbindRequest request = new CustomerUnbindRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setOutShopId(shopId);
            request.setNasOuid(nasOuid);
            request.setMobile(nasOuid);
            // platform=11 与 sync-vips 注册时的 subPlatform 一致（有赞渠道）
            request.setPlatform(0);

            ApiClient apiClient = new ApiClientImpl(request);
            CustomerUnbindResponse response = apiClient.execute(request);

            if (response == null) {
                throw new RuntimeException("无响应");
            }

            UnbindResponse parsed = new UnbindResponse();
            parsed.code      = response.getCode();
            parsed.msg       = response.getMsg();
            parsed.result    = response.getResult();
            parsed.requestId = response.getRequestId();
            parsed.rawBody   = response.getBody();
            parsed.success   = Boolean.TRUE.equals(response.success);

            if (parsed.success) {
                log.info("会员解绑成功, shopId={}, nasOuid={}, code={}, msg={}, response={}",
                    shopId, nasOuid, parsed.code, parsed.msg, parsed.rawBody);
            } else {
                // 业务失败（如"会员不存在"）：不抛异常，返回给调用方，由业务逻辑决定是否标记失败
                log.warn("会员解绑业务失败, shopId={}, nasOuid={}, code={}, msg={}, response={}",
                    shopId, nasOuid, parsed.code, parsed.msg, parsed.rawBody);
            }
            return parsed;
        } catch (Exception e) {
            // 系统异常（网络超时、SDK 错误等）：抛出，由调用方重试
            log.error("会员解绑系统异常, shopId={}, nasOuid={}: {}", shopId, nasOuid, e.getMessage(), e);
            throw new RuntimeException("会员解绑系统异常: " + e.getMessage(), e);
        }
    }

    /**
     * 南讯解绑接口响应解析结果。
     * <p>字段来自 {@code BaseResponse<String>} 的公共字段。
     */
    public static final class UnbindResponse {
        /** 是否业务成功（来自 response.success） */
        public boolean success;
        /** 状态码（如 "200"） */
        public String code;
        /** 提示信息 */
        public String msg;
        /** 接口返回的业务数据（解绑成功时可能为 "解绑成功" 等字符串） */
        public String result;
        /** 请求追踪 ID（排查用） */
        public String requestId;
        /** 原始 JSON body（兜底保留） */
        public String rawBody;

        @Override
        public String toString() {
            return "{code=" + code + ", msg=" + msg + ", result=" + result + ", requestId=" + requestId + "}";
        }
    }

    /**
     * 批量保存南讯会员基本信息
     *
     * @param customers 会员信息列表（每条包含 nasOuid/mobile/customerName 等）
     * @param shopId    外部门店 ID（outShopId）
     * @return 批次内成功条数（response.success=true 时按 total - customerSaveFailedList.size() 计算）
     */
    public int saveCustomers(List<CustomerSaveInfo> customers, String shopId) {
        String token = getAccessToken();
        try {
            BatchCustomerSaveRequest request = new BatchCustomerSaveRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setCustomerSaveList(customers);
            request.setOutShopId(shopId);

            ApiClient apiClient = new ApiClientImpl(request);
            BatchCustomerSaveResponse response = apiClient.execute(request);

            // B1: BaseResponse.success 是 Boolean 包装类，可能为 null，必须用 Boolean.TRUE.equals 防拆包 NPE
            if (response == null || !Boolean.TRUE.equals(response.success)) {
                throw new RuntimeException("会员同步失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }

            int total = customers.size();
            List<com.nascent.ecrp.opensdk.domain.customer.CustomerSaveFailed> failedList =
                response.getCustomerSaveFailedList();
            int failedCount = (failedList == null) ? 0 : failedList.size();
            if (failedCount > 0) {
                failedList.forEach(f -> log.warn("会员注册失败, shopId={}, nasOuid={}, msg={}",
                    shopId, f.getNasOuid(), f.getFailedMessage()));
            }
            // S5: 防御 SDK 异常返回（failedCount > total）
            if (failedCount > total) {
                log.warn("南讯返回失败数({})大于提交总数({}), shopId={}, 已截断为0成功",
                    failedCount, total, shopId);
            }
            int successCount = Math.max(0, total - failedCount);
            log.info("会员同步成功, shopId={}, 总{}条, 成功{}条, 失败{}条",
                shopId, total, successCount, failedCount);
            return successCount;
        } catch (Exception e) {
            // B3: 把原始 message 拼入外层异常，便于上层 retry 日志看到根因
            log.error("会员同步失败, shopId={}: {}", shopId, e.getMessage(), e);
            throw new RuntimeException("会员同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量更新南讯会员等级
     *
     * @param gradeUpdates 等级更新列表（每条含 nasOuid + grade）
     * @param shopId       外部门店 ID（outShopId）
     * @return 批次内成功条数（基于 response.result.successList 计算）
     */
    public int updateCustomerGrades(List<CustomerGradeUpdateInfo> gradeUpdates, String shopId) {
        String token = getAccessToken();
        try {
            CustomerGradeUpdateRequest request = new CustomerGradeUpdateRequest();
            request.setAppKey(appKey);
            request.setGroupId(groupId);
            request.setAppSecret(appSecret);
            request.setServerUrl(serverUrl);
            request.setAccessToken(token);
            request.setOutShopId(shopId);
            request.setCustomerGradeUpdateInfoList(gradeUpdates);

            ApiClient apiClient = new ApiClientImpl(request);
            CustomerGradeUpdateResponse response = apiClient.execute(request);

            // B1: Boolean 包装类防拆包 NPE
            if (response == null || !Boolean.TRUE.equals(response.success)) {
                throw new RuntimeException("会员等级更新失败: " +
                    (response != null ? response.getBody() : "无响应"));
            }

            com.nascent.ecrp.opensdk.domain.customer.CustomerGradeUpdateResult result =
                response.getResult();
            int total = gradeUpdates.size();
            int successCount;
            int failedCount;
            if (result == null) {
                // SDK 未返回明细，按整批成功计
                successCount = total;
                failedCount = 0;
            } else {
                List<String> successList = result.getSuccessList();
                successCount = (successList == null) ? 0 : successList.size();
                List<com.nascent.ecrp.opensdk.domain.customer.CustomerGradeUpdateResultInfo> failedList =
                    result.getFailedList();
                failedCount = (failedList == null) ? 0 : failedList.size();
                if (failedCount > 0) {
                    failedList.forEach(f -> log.warn("会员等级更新失败, shopId={}, 详情={}",
                        shopId, f));
                }
                // M2: SDK 返回明细总数与提交总数一致性校验
                int reportedTotal = successCount + failedCount;
                if (reportedTotal != total) {
                    log.warn("南讯返回明细总数({})与提交总数({})不一致, shopId={}, 可能存在未明状态条目",
                        reportedTotal, total, shopId);
                }
            }
            log.info("会员等级更新成功, shopId={}, 总{}条, 成功{}条, 失败{}条",
                shopId, total, successCount, failedCount);
            return successCount;
        } catch (Exception e) {
            // B3: 原始 message 拼入外层异常
            log.error("会员等级更新失败, shopId={}: {}", shopId, e.getMessage(), e);
            throw new RuntimeException("会员等级更新失败: " + e.getMessage(), e);
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

            if (response == null || !Boolean.TRUE.equals(response.success)) {
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
