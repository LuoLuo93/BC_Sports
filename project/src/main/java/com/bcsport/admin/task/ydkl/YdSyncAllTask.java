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
        // 客流与天气互不依赖：一个失败不能跳过另一个，但任务整体要标记失败告警
        RuntimeException firstError = null;
        try {
            customerFlowTask.sync();
        } catch (Exception e) {
            firstError = new RuntimeException("客流数据同步失败: " + e.getMessage(), e);
        }
        try {
            weatherTask.sync();
        } catch (Exception e) {
            if (firstError == null) {
                firstError = new RuntimeException("天气数据同步失败: " + e.getMessage(), e);
            } else {
                firstError.addSuppressed(e);
            }
        }
        if (firstError != null) {
            log.error("=== 失败: 云盯一键同步 ===", firstError);
            throw firstError;
        }
        log.info("=== 完成: 云盯一键同步(客流+天气) ===");
    }

    /**
     * 带参数同步：支持日期范围
     * 参数: startTime=yyyy-MM-dd, endTime=yyyy-MM-dd，为空默认前一天
     */
    public void syncAll(Map<String, String> params) {
        log.info("=== 开始执行: 云盯一键同步(客流+天气, 带参数) ===");
        RuntimeException firstError = null;
        try {
            customerFlowTask.sync(params);
        } catch (Exception e) {
            firstError = new RuntimeException("客流数据同步失败: " + e.getMessage(), e);
        }
        try {
            weatherTask.sync(params);
        } catch (Exception e) {
            if (firstError == null) {
                firstError = new RuntimeException("天气数据同步失败: " + e.getMessage(), e);
            } else {
                firstError.addSuppressed(e);
            }
        }
        if (firstError != null) {
            log.error("=== 失败: 云盯一键同步(带参数) ===", firstError);
            throw firstError;
        }
        log.info("=== 完成: 云盯一键同步(客流+天气, 带参数) ===");
    }
}
