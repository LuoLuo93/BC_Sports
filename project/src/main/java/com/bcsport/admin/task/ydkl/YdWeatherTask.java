package com.bcsport.admin.task.ydkl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.dto.ydkl.*;
import com.bcsport.admin.entity.ydkl.YdWeather;
import com.bcsport.admin.ydklmapper.YdWeatherMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 云盯天气任务：同步昨日门店天气数据
 */
@Slf4j
@Component("ydWeatherTask")
public class YdWeatherTask {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private YdApiClient apiClient;

    @Autowired
    private YdWeatherMapper weatherMapper;

    @Autowired
    @Qualifier("ihrTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("=== 开始执行: 云盯同步天气数据 ===");
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

            YdWeatherRequest request = new YdWeatherRequest();
            request.setStoreIds(shopIds);
            request.setStartTime(startTime.getTime());
            request.setEndTime(endTime.getTime());

            // 5. 分页获取所有数据
            int pageNo = 0;
            int pageSize = 100;
            YdWeatherResponse firstResp = apiClient.getWeatherData(token, request, pageNo, pageSize);
            if (firstResp == null || firstResp.getMetadata() == null) {
                log.warn("=== 完成: 获取天气数据为空 ===");
                return;
            }

            int totalPages = firstResp.getMetadata().getTotalPages();
            log.info("天气数据共{}页", totalPages);

            // 收集所有页数据
            List<YdWeatherResponse.Content> allContents = new ArrayList<>();
            if (CollUtil.isNotEmpty(firstResp.getContent())) {
                allContents.addAll(firstResp.getContent());
            }

            for (int page = 1; page < totalPages; page++) {
                YdWeatherResponse resp = apiClient.getWeatherData(token, request, page, pageSize);
                if (resp != null && CollUtil.isNotEmpty(resp.getContent())) {
                    allContents.addAll(resp.getContent());
                }
            }

            log.info("共获取{}条天气数据", allContents.size());

            // --- DB writes: short transaction ---
            Date dataDate = DateUtil.yesterday();
            new TransactionTemplate(transactionManager).execute(status -> {
                weatherMapper.deleteByDate(DateUtil.format(dataDate, "yyyy-MM-dd"));
                insertBatch(allContents, dataDate);
                return null;
            });

            log.info("=== 完成: 云盯同步天气数据 ===");
        } catch (Exception e) {
            log.error("=== 失败: 云盯同步天气数据: {} ===", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 带参数同步：支持日期范围，逐天删除+重新获取
     * 参数: startTime=yyyy-MM-dd, endTime=yyyy-MM-dd，为空默认前一天
     */
    public void sync(Map<String, String> params) {
        log.info("=== 开始执行: 云盯同步天气数据(带参数) ===");
        try {
            Date startDate = parseDate(params, "startTime");
            Date endDate = parseDate(params, "endTime");
            if (startDate == null && endDate == null) {
                sync();
                return;
            }
            if (startDate == null) startDate = endDate;
            if (endDate == null) endDate = startDate;

            YdTokenResponse token = apiClient.getToken();
            if (token == null || ObjectUtil.isEmpty(token.getAccessToken())) {
                log.error("=== 失败: 获取云盯Token ===");
                return;
            }
            List<YdShopInfo> shops = apiClient.getShopList(token);
            if (CollUtil.isEmpty(shops)) {
                log.warn("=== 完成: 无安装客流统计的门店 ===");
                return;
            }
            List<String> shopIds = apiClient.extractShopIds(shops);
            log.info("获取到{}个门店", shopIds.size());

            Date current = DateUtil.beginOfDay(startDate);
            Date endDay = DateUtil.endOfDay(endDate);
            while (!current.after(endDay)) {
                String dateStr = DateUtil.format(current, "yyyy-MM-dd");
                log.info("同步天气数据: {}", dateStr);

                DateTime dayStart = DateUtil.beginOfDay(current);
                DateTime dayEnd = DateUtil.endOfDay(current);

                YdWeatherRequest request = new YdWeatherRequest();
                request.setStoreIds(shopIds);
                request.setStartTime(dayStart.getTime());
                request.setEndTime(dayEnd.getTime());

                int pageNo = 0;
                int pageSize = 100;
                YdWeatherResponse firstResp = apiClient.getWeatherData(token, request, pageNo, pageSize);
                if (firstResp == null || firstResp.getMetadata() == null) {
                    log.warn("{}: 获取天气数据为空，跳过", dateStr);
                    current = DateUtil.offsetDay(current, 1);
                    continue;
                }

                int totalPages = firstResp.getMetadata().getTotalPages();
                List<YdWeatherResponse.Content> allContents = new ArrayList<>();
                if (CollUtil.isNotEmpty(firstResp.getContent())) {
                    allContents.addAll(firstResp.getContent());
                }
                for (int page = 1; page < totalPages; page++) {
                    YdWeatherResponse resp = apiClient.getWeatherData(token, request, page, pageSize);
                    if (resp != null && CollUtil.isNotEmpty(resp.getContent())) {
                        allContents.addAll(resp.getContent());
                    }
                }
                log.info("{}: 获取{}条天气数据", dateStr, allContents.size());

                Date finalCurrent = current;
                new TransactionTemplate(transactionManager).execute(status -> {
                    weatherMapper.deleteByDate(DateUtil.format(finalCurrent, "yyyy-MM-dd"));
                    insertBatch(allContents, finalCurrent);
                    return null;
                });

                current = DateUtil.offsetDay(current, 1);
            }

            log.info("=== 完成: 云盯同步天气数据(带参数) ===");
        } catch (Exception e) {
            log.error("=== 失败: 云盯同步天气数据(带参数): {} ===", e.getMessage(), e);
            throw e;
        }
    }

    private Date parseDate(Map<String, String> params, String key) {
        if (params == null) return null;
        String val = params.get(key);
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return DateUtil.parse(val, "yyyy-MM-dd HH:mm:ss");
        } catch (Exception e) {
            try {
                return DateUtil.parse(val, "yyyy-MM-dd");
            } catch (Exception e2) {
                log.warn("无法解析日期参数 {}: {}", key, val);
                return null;
            }
        }
    }

    private void insertBatch(List<YdWeatherResponse.Content> contents, Date dataDate) {
        List<YdWeather> list = new ArrayList<>();
        for (YdWeatherResponse.Content content : contents) {
            YdWeather weather = new YdWeather();
            weather.setId(IdWorker.getId());
            weather.setStoreIdUuid(content.getStoreIdUuid());
            weather.setStoreCode(content.getStoreCode());
            weather.setStorename(content.getStorename());
            weather.setRealTime(content.getRealTime());
            weather.setProvince(content.getProvince());
            weather.setCity(content.getCity());
            weather.setWeather(content.getWeather());
            weather.setMinTemp(content.getMinTemp());
            weather.setMaxTemp(content.getMaxTemp());
            weather.setWind(content.getWind());
            weather.setInsertDate(dataDate);
            list.add(weather);
        }
        // 分批插入
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, list.size());
            weatherMapper.insertBatch(list.subList(i, end));
        }
        log.info("天气数据入库{}条", list.size());
    }
}
