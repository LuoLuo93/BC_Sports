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

    public void sync() {
        log.info("=== 开始执行: 同步客户联系成员 ===");
        try {
            List<String> followUserList = apiClient.getFollowUserList();

            if (followUserList == null || followUserList.isEmpty()) {
                log.info("=== 完成: 同步客户联系成员, 无数据 ===");
                return;
            }

            List<QywxFollowUser> entityList = new ArrayList<>();
            for (String userId : followUserList) {
                QywxFollowUser entity = new QywxFollowUser();
                entity.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                entity.setFollowUser(userId);
                entityList.add(entity);
            }

            doSync(entityList);

            log.info("=== 完成: 同步客户联系成员, 共 {} 条 ===", entityList.size());
        } catch (Exception e) {
            log.error("=== 失败: 同步客户联系成员 ===", e);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void doSync(List<QywxFollowUser> entityList) {
        followUserMapper.deleteAll();
        for (int i = 0; i < entityList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entityList.size());
            followUserMapper.insertBatch(entityList.subList(i, end));
        }
    }
}
