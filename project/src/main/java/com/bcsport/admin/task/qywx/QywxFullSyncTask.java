package com.bcsport.admin.task.qywx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 企业微信一键同步任务
 *
 * 按顺序执行：
 * 1. 同步部门
 * 2. 同步部门成员
 * 3. 同步成员详情（API直连方式）
 * 4. 同步成员扩展属性
 *
 * 注意：编排方法不加@Transactional，每个子步骤有独立事务，
 * 单步失败不影响其他步骤，避免嵌套事务导致UnexpectedRollbackException
 */
@Slf4j
@Component("qywxFullSyncTask")
public class QywxFullSyncTask {

    private static volatile boolean isSyncing = false;

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
    public void syncAll() {
        synchronized (QywxFullSyncTask.class) {
            if (isSyncing) {
                log.warn("企业微信一键同步正在进行中，请勿重复操作");
                return;
            }
            isSyncing = true;
        }
        log.info("=== 开始执行：企业微信一键同步 ===");
        long totalStartTime = System.currentTimeMillis();

        try {
            int successCount = 0;
            int failCount = 0;

            // 步骤1：同步部门
            log.info("--- 步骤 1/4：同步部门 ---");
            try {
                departmentTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 1/4：同步部门 失败 ---", e);
            }

            // 步骤2：同步部门成员
            log.info("--- 步骤 2/4：同步部门成员 ---");
            try {
                departmentMemberTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 2/4：同步部门成员 失败 ---", e);
            }

            // 步骤3：同步成员详情（API直连方式，不依赖本地数据）
            log.info("--- 步骤 3/4：同步成员详情 ---");
            try {
                detailTask.syncFromApi();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 3/4：同步成员详情 失败 ---", e);
            }

            // 步骤4：同步成员扩展属性
            log.info("--- 步骤 4/4：同步成员扩展属性 ---");
            try {
                attrsBaseTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 4/4：同步成员扩展属性 失败 ---", e);
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 企业微信一键同步 完成, 成功: {}, 失败: {}, 耗时: {} ms ===",
                    successCount, failCount, totalTime);

            if (failCount > 0) {
                throw new RuntimeException("企业微信一键同步部分失败: " + failCount + "个步骤失败");
            }

        } catch (Exception e) {
            log.error("=== 企业微信一键同步 异常 ===", e);
            throw e;
        } finally {
            synchronized (QywxFullSyncTask.class) {
                isSyncing = false;
            }
        }
    }

    public static boolean isSyncing() {
        return isSyncing;
    }
}
