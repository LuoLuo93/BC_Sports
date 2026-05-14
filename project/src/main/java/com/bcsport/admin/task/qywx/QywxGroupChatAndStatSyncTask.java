package com.bcsport.admin.task.qywx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 企业微信群聊相关整合同步任务
 *
 * 按顺序执行：
 * 1. 同步配置了客户联系功能的成员列表
 * 2. 同步群聊列表和群成员
 * 3. 同步群聊统计数据
 * 4. 同步群发消息记录
 * 5. 同步朋友圈
 */
@Slf4j
@Component("qywxGroupChatAndStatSyncTask")
public class QywxGroupChatAndStatSyncTask {

    @Autowired
    private QywxFollowUserTask followUserTask;

    @Autowired
    private QywxGroupChatTask groupChatTask;

    @Autowired
    private QywxGroupChatStatTask groupChatStatTask;

    @Autowired
    private QywxMassMessageTask massMessageTask;

    @Autowired
    private QywxMomentTask momentTask;

    /**
     * 一键同步所有群聊相关数据
     */
    public void syncAll() {
        log.info("=== 开始执行：企业微信群聊整合同步 ===");
        long totalStartTime = System.currentTimeMillis();

        try {
            int successCount = 0;
            int failCount = 0;

            // 步骤1：同步配置了客户联系功能的成员列表
            log.info("--- 步骤 1/5：同步客户联系成员 ---");
            try {
                followUserTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 1/5：同步客户联系成员 失败 ---", e);
            }

            // 步骤2：同步群聊列表和群成员
            log.info("--- 步骤 2/5：同步企微群聊 ---");
            try {
                groupChatTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 2/5：同步企微群聊 失败 ---", e);
            }

            // 步骤3：同步群聊统计数据
            log.info("--- 步骤 3/5：同步群聊统计 ---");
            try {
                groupChatStatTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 3/5：同步群聊统计 失败 ---", e);
            }

            // 步骤4：同步群发消息记录
            log.info("--- 步骤 4/5：同步群发消息 ---");
            try {
                massMessageTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 4/5：同步群发消息 失败 ---", e);
            }

            // 步骤5：同步朋友圈
            log.info("--- 步骤 5/5：同步朋友圈 ---");
            try {
                momentTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 5/5：同步朋友圈 失败 ---", e);
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 企业微信群聊整合同步 完成, 成功: {}, 失败: {}, 耗时: {} ms ===",
                    successCount, failCount, totalTime);

        } catch (Exception e) {
            log.error("=== 企业微信群聊整合同步 异常 ===", e);
            throw e;
        }
    }
}
