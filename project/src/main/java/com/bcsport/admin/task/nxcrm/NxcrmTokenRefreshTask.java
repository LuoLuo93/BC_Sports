package com.bcsport.admin.task.nxcrm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("nxcrmTokenRefreshTask")
public class NxcrmTokenRefreshTask {

    @Resource
    private NxCrmApiClient nxCrmApiClient;

    public void refreshToken() {
        log.info("=== 开始执行: 南讯CRM刷新Token ===");
        try {
            nxCrmApiClient.refreshToken();
            log.info("=== 完成执行: 南讯CRM刷新Token ===");
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM刷新Token ===", e);
            throw e;
        }
    }
}
