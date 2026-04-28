package com.bcsport.admin.task.qywx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业微信一键同步任务
 *
 * 按顺序执行：
 * 1. 同步部门
 * 2. 同步部门成员
 * 3. 同步成员详情（API直连方式）
 * 4. 同步成员扩展属性
 */
@Slf4j
@Component("qywxFullSyncTask")
public class QywxFullSyncTask {

    @Autowired
    private QywxDepartmentTask departmentTask;

    @Autowired
    private QywxDepartmentMemberTask departmentMemberTask;

    @Autowired
    private QywxDepartmentMemberDetailTask detailTask;

    @Autowired
    private QywxAttrsBaseTask attrsBaseTask;

    /**
     * 一键同步所有企业微信数据
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void syncAll() {
        log.info("========================================");
        log.info("=== 开始执行：企业微信一键同步 ===");
        log.info("========================================");
        long totalStartTime = System.currentTimeMillis();

        try {
            int successCount = 0;
            int failCount = 0;

            // 步骤1：同步部门
            log.info("--- 步骤 1/4：同步部门 ---");
            try {
                departmentTask.sync();
                successCount++;
                log.info("--- 步骤 1/4：同步部门 完成 ---");
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 1/4：同步部门 失败 ---", e);
            }

            // 步骤2：同步部门成员
            log.info("--- 步骤 2/4：同步部门成员 ---");
            try {
                departmentMemberTask.sync();
                successCount++;
                log.info("--- 步骤 2/4：同步部门成员 完成 ---");
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 2/4：同步部门成员 失败 ---", e);
            }

            // 步骤3：同步成员详情（API直连方式，不依赖本地数据）
            log.info("--- 步骤 3/4：同步成员详情 ---");
            try {
                detailTask.syncFromApi();
                successCount++;
                log.info("--- 步骤 3/4：同步成员详情 完成 ---");
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 3/4：同步成员详情 失败 ---", e);
            }

            // 步骤4：同步成员扩展属性
            log.info("--- 步骤 4/4：同步成员扩展属性 ---");
            try {
                attrsBaseTask.sync();
                successCount++;
                log.info("--- 步骤 4/4：同步成员扩展属性 完成 ---");
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 4/4：同步成员扩展属性 失败 ---", e);
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("========================================");
            log.info("=== 企业微信一键同步 完成 ===");
            log.info("=== 成功：{} 个步骤，失败：{} 个步骤 ===", successCount, failCount);
            log.info("=== 总耗时：{} ms ===", totalTime);
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("=== 企业微信一键同步 异常 ===", e);
            log.error("========================================");
            throw e;
        }
    }
}
