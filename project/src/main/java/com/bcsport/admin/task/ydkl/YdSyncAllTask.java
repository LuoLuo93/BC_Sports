package com.bcsport.admin.task.ydkl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 云盯一键同步任务：客流数据 → 天气数据
 */
@Slf4j
@Component("ydSyncAllTask")
public class YdSyncAllTask {

    @Autowired
    private YdCustomerFlowTask customerFlowTask;

    @Autowired
    private YdWeatherTask weatherTask;

    public void syncAll() {
        log.info("=== 开始执行: 云盯一键同步(客流+天气) ===");
        try {
            customerFlowTask.sync();
            weatherTask.sync();
            log.info("=== 完成: 云盯一键同步(客流+天气) ===");
        } catch (Exception e) {
            log.error("=== 失败: 云盯一键同步: {} ===", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 带参数同步：支持日期范围
     * 参数: startTime=yyyy-MM-dd, endTime=yyyy-MM-dd，为空默认前一天
     */
    public void syncAll(Map<String, String> params) {
        log.info("=== 开始执行: 云盯一键同步(客流+天气, 带参数) ===");
        try {
            customerFlowTask.sync(params);
            weatherTask.sync(params);
            log.info("=== 完成: 云盯一键同步(客流+天气, 带参数) ===");
        } catch (Exception e) {
            log.error("=== 失败: 云盯一键同步(带参数): {} ===", e.getMessage(), e);
            throw e;
        }
    }
}
