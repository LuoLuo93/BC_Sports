package com.bcsport.admin.task.qywx;

import com.bcsport.admin.entity.qywx.QywxFollowUser;
import com.bcsport.admin.qywxmapper.QywxFollowUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业微信任务：同步配置了客户联系功能的成员列表
 */
@Slf4j
@Component("qywxFollowUserTask")
public class QywxFollowUserTask {

    private static final int BATCH_SIZE = 50;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxFollowUserMapper followUserMapper;

    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void sync() {
        log.info("=== Starting: QYWX sync follow user list ===");
        try {
            List<String> followUserList = apiClient.getFollowUserList();

            if (followUserList == null || followUserList.isEmpty()) {
                log.info("=== Completed: QYWX sync follow user list, no data ===");
                return;
            }

            followUserMapper.deleteAll();

            // 转换为实体列表
            List<QywxFollowUser> entityList = new ArrayList<>();
            for (String userId : followUserList) {
                QywxFollowUser entity = new QywxFollowUser();
                entity.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                entity.setFollowUser(userId);
                entityList.add(entity);
            }

            // 批量插入
            for (int i = 0; i < entityList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, entityList.size());
                followUserMapper.insertBatch(entityList.subList(i, end));
            }

            log.info("=== Completed: QYWX sync follow user list, total {} ===", entityList.size());
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync follow user list ===", e);
            throw e;
        }
    }
}
