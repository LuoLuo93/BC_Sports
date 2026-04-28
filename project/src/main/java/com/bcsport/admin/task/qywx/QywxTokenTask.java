package com.bcsport.admin.task.qywx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业微信任务：刷新AccessToken
 */
@Slf4j
@Component("qywxTokenTask")
public class QywxTokenTask {

    @Autowired
    private QywxApiClient apiClient;

    /**
     * 刷新AccessToken
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void refresh() {
        log.info("=== 开始执行: QYWX刷新Token ===");
        try {
            String token = apiClient.refreshToken();
            log.info("=== 完成执行: QYWX刷新Token ===");
        } catch (Exception e) {
            log.error("=== 失败执行: QYWX刷新Token ===", e);
            throw e;
        }
    }
}
