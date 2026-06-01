package com.bcsport.admin.task.qywx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 企业微信员工生命周期整合同步任务
 *
 * 按顺序执行：
 * 1. 同步企微基础信息（一键同步）
 * 2. 录入企微新员工
 * 3. 更新企微员工信息
 * 4. 企微员工离职同步
 */
@Slf4j
@Component("qywxEmployeeLifecycleSyncTask")
public class QywxEmployeeLifecycleSyncTask {

    private static volatile boolean syncing = false;

    public static boolean isSyncing() {
        return syncing;
    }

    @Autowired
    private QywxFullSyncTask qywxFullSyncTask;

    @Autowired
    private QywxNewEmployeeSyncTask newEmployeeSyncTask;

    @Autowired
    private QywxEmployeeUpdateSyncTask employeeUpdateSyncTask;

    @Autowired
    private QywxEmployeeLeaveSyncTask employeeLeaveSyncTask;

    /**
     * 一键同步员工生命周期所有数据
     */
    public void syncAll() {
        synchronized (QywxEmployeeLifecycleSyncTask.class) {
            if (syncing) {
                log.warn("企业微信员工生命周期整合同步正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行：企业微信员工生命周期整合同步 ===");
        long totalStartTime = System.currentTimeMillis();
        try {
            int successCount = 0;
            int failCount = 0;

            // 步骤1：同步企微基础信息（一键同步）
            log.info("--- 步骤 1/4：同步企微基础信息 ---");
            try {
                qywxFullSyncTask.syncAll();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 1/4：同步企微基础信息 失败 ---", e);
            }

            // 步骤2：录入企微新员工
            log.info("--- 步骤 2/4：录入企微新员工 ---");
            try {
                newEmployeeSyncTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 2/4：录入企微新员工 失败 ---", e);
            }

            // 步骤3：更新企微员工信息
            log.info("--- 步骤 3/4：更新企微员工信息 ---");
            try {
                employeeUpdateSyncTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 3/4：更新企微员工信息 失败 ---", e);
            }

            // 步骤4：企微员工离职同步
            log.info("--- 步骤 4/4：企微员工离职同步 ---");
            try {
                employeeLeaveSyncTask.sync();
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("--- 步骤 4/4：企微员工离职同步 失败 ---", e);
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 企业微信员工生命周期整合同步 完成, 成功: {}, 失败: {}, 耗时: {} ms ===",
                    successCount, failCount, totalTime);

        } catch (Exception e) {
            log.error("=== 企业微信员工生命周期整合同步 异常 ===", e);
            throw e;
        } finally {
            synchronized (QywxEmployeeLifecycleSyncTask.class) {
                syncing = false;
            }
        }
    }
}
