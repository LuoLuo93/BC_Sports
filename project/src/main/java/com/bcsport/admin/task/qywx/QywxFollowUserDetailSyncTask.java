package com.bcsport.admin.task.qywx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 企业微信客户联系成员整合同步任务
 *
 * 按顺序执行：
 * 1. 同步客户联系成员列表
 * 2. 同步客户联系成员详情
 * 3. 同步客户详情
 *
 * 注意：编排方法不加@Transactional，每个子步骤有独立事务，
 * 单步失败不影响其他步骤，避免嵌套事务导致UnexpectedRollbackException
 */
@Slf4j
@Component("qywxFollowUserDetailSyncTask")
public class QywxFollowUserDetailSyncTask {

    @Autowired
    private QywxFollowUserTask followUserTask;

    @Autowired
    private QywxDepartmentMemberDetailTask detailTask;

    @Autowired
    private QywxCustomerDetailTask customerDetailTask;

    /**
     * 一键同步客户联系成员及其详情
     */
    public void syncAll() {
        log.info("=== 开始执行：企业微信客户联系成员整合同步 ===");
        long totalStartTime = System.currentTimeMillis();

        try {
            int successCount = 0;
            int failCount = 0;

            // 步骤1：同步客户联系成员列表
            log.info("--- 步骤 1/3：同步客户联系成员 ---");
            try {
                followUserTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 1/3：同步客户联系成员 失败 ---", e);
            }

            // 步骤2：同步客户联系成员详情
            log.info("--- 步骤 2/3：同步客户联系成员详情 ---");
            try {
                detailTask.syncFromFollowUser();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 2/3：同步客户联系成员详情 失败 ---", e);
            }

            // 步骤3：同步客户详情
            log.info("--- 步骤 3/3：同步客户详情 ---");
            try {
                customerDetailTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 3/3：同步客户详情 失败 ---", e);
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 企业微信客户联系成员整合同步 完成, 成功: {}, 失败: {}, 耗时: {} ms ===",
                    successCount, failCount, totalTime);

        } catch (Exception e) {
            log.error("=== 企业微信客户联系成员整合同步 异常 ===", e);
            throw e;
        }
    }
}
