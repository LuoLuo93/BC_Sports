package com.bcsport.admin.task.ydkl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.dto.ydkl.*;
import com.bcsport.admin.entity.ydkl.YdCustomerFlow;
import com.bcsport.admin.ydklmapper.YdCustomerFlowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 云盯客流任务：同步昨日门店客流数据
 */
@Slf4j
@Component("ydCustomerFlowTask")
public class YdCustomerFlowTask {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private YdApiClient apiClient;

    @Autowired
    private YdCustomerFlowMapper customerFlowMapper;

    @Autowired
    @Qualifier("ihrTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("=== 开始执行: 云盯同步客流数据 ===");
        try {
            // --- HTTP calls: outside transaction ---
            // 1. 获取Token
            YdTokenResponse token = apiClient.getToken();
            if (token == null || ObjectUtil.isEmpty(token.getAccessToken())) {
                log.error("=== 失败: 获取云盯Token失败 ===");
                return;
            }

            // 2. 获取门店列表
            List<YdShopInfo> shops = apiClient.getShopList(token);
            if (CollUtil.isEmpty(shops)) {
                log.warn("=== 完成: 无安装客流统计的门店 ===");
                return;
            }
            List<String> shopIds = apiClient.extractShopIds(shops);
            log.info("获取到{}个门店", shopIds.size());

            // 4. 构建请求参数（昨日数据）
            DateTime startTime = DateUtil.beginOfDay(DateUtil.yesterday());
            DateTime endTime = DateUtil.endOfDay(DateUtil.yesterday());

            YdCustomerFlowRequest request = new YdCustomerFlowRequest();
            request.setStoreIds(shopIds);
            request.setStartTime(startTime.getTime());
            request.setEndTime(endTime.getTime());
            request.setTimeType("DAY");

            // 5. 分页获取所有数据
            int pageNo = 0;
            int pageSize = 100;
            YdCustomerFlowResponse firstResp = apiClient.getCustomerFlowData(token, request, pageNo, pageSize);
            if (firstResp == null || firstResp.getMetadata() == null) {
                log.warn("=== 完成: 获取客流数据为空 ===");
                return;
            }

            int totalPages = firstResp.getMetadata().getTotalPages();
            log.info("客流数据共{}页", totalPages);

            // 收集所有页数据
            List<YdCustomerFlowResponse.Content> allContents = new ArrayList<>();
            if (CollUtil.isNotEmpty(firstResp.getContent())) {
                allContents.addAll(firstResp.getContent());
            }

            for (int page = 1; page < totalPages; page++) {
                YdCustomerFlowResponse resp = apiClient.getCustomerFlowData(token, request, page, pageSize);
                if (resp != null && CollUtil.isNotEmpty(resp.getContent())) {
                    allContents.addAll(resp.getContent());
                }
            }

            log.info("共获取{}条客流数据", allContents.size());

            // --- DB writes: short transaction ---
            new TransactionTemplate(transactionManager).execute(status -> {
                // 3. 清除今天的旧数据
                customerFlowMapper.deleteTodayData();
                insertBatch(allContents);
                return null;
            });

            log.info("=== 完成: 云盯同步客流数据 ===");
        } catch (Exception e) {
            log.error("=== 失败: 云盯同步客流数据: {} ===", e.getMessage(), e);
            throw e;
        }
    }

    private void insertBatch(List<YdCustomerFlowResponse.Content> contents) {
        List<YdCustomerFlow> list = new ArrayList<>();
        for (YdCustomerFlowResponse.Content content : contents) {
            YdCustomerFlow flow = new YdCustomerFlow();
            flow.setId(IdWorker.getId());
            flow.setStoreIdUuid(content.getStoreIdUuid());
            flow.setStoreCode(content.getStoreCode());
            flow.setName(content.getName());
            flow.setRealTime(content.getRealTime());
            flow.setIndoorCount(content.getIndoorCount());
            flow.setOutdoorCount(content.getOutdoorCount());
            flow.setStatDimensionDayTime(content.getStatDimensionDayTime());
            flow.setStatDimensionHourTime(content.getStatDimensionHourTime());
            flow.setOutsum(content.getOutsum());
            flow.setAreaCode(content.getAreaCode());
            flow.setAreaName(content.getAreaName());
            flow.setStoreAreaIdUuid(content.getStoreAreaIdUuid());
            list.add(flow);
        }
        // 分批插入
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, list.size());
            customerFlowMapper.insertBatch(list.subList(i, end));
        }
        log.info("客流数据入库{}条", list.size());
    }
}
