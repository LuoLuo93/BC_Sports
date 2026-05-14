package com.bcsport.admin.task.ihr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IHR任务：刷新 OAuth Token
 */
@Slf4j
@Component("ihrTokenTask")
public class IhrTokenTask {

    @Autowired
    private IhrApiClient apiClient;

    /**
     * 刷新 Token 并持久化到数据库
     * apiClient.refreshToken() 内部自行管理数据库操作，无需外部事务
     */
    public void refresh() {
        log.info("=== 开始执行: IHR刷新Token ===");
        try {
            String token = apiClient.refreshToken();
            log.info("=== 完成: IHR刷新Token, token长度: {} ===", token.length());
        } catch (Exception e) {
            log.error("=== 失败: IHR刷新Token: {} ===", e.getMessage());
            throw e;
        }
    }
}
