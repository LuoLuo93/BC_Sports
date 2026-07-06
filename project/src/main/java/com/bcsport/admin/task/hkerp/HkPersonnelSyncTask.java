package com.bcsport.admin.task.hkerp;

import com.bcsport.admin.service.HkPersonnelSyncService;
import com.bcsport.admin.vo.HkSyncStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 旧版HK ERP 职员资料同步任务（HK员工生命周期）
 * <p>
 * 数据流：BC_SPORTS_IHR（终端店铺部门过滤）→ HKERP（Bas_Personnel 直写）
 * <ul>
 *   <li>{@link #syncAll()} 员工生命周期：顺序执行 入职 → 变更 → 离职（推荐，定时任务/一键同步入口）</li>
 *   <li>{@link #syncOnboarding()} 仅入职同步（前端按钮/单条触发）</li>
 *   <li>{@link #syncUpdate()} 仅变更+离职同步（前端按钮）</li>
 * </ul>
 * 通过 ScheduleTaskRegistry + schedule_job 表配置 cron，支持手动触发。
 * syncAll 与 syncOnboarding/syncUpdate 共用互斥锁，避免并发执行。
 */
@Slf4j
@Component("hkPersonnelSyncTask")
public class HkPersonnelSyncTask {

    private static volatile boolean lifecycleSyncing = false;
    private static volatile boolean onboardingSyncing = false;
    private static volatile boolean updateSyncing = false;
    private static volatile HkSyncStatsVO lastOnboardingStats;
    private static volatile HkSyncStatsVO lastUpdateStats;
    private static volatile Date lastOnboardingTime;
    private static volatile Date lastUpdateTime;
    private static volatile Date lastLifecycleTime;

    @Autowired
    private HkPersonnelSyncService syncService;

    /** 生命周期整体进行中（入职+变更+离职顺序执行） */
    public static boolean isLifecycleSyncing() {
        return lifecycleSyncing;
    }

    public static boolean isOnboardingSyncing() {
        return onboardingSyncing;
    }

    public static boolean isUpdateSyncing() {
        return updateSyncing;
    }

    /** 当前是否任意阶段进行中（生命周期或任意子阶段） */
    public static boolean isAnySyncing() {
        return lifecycleSyncing || onboardingSyncing || updateSyncing;
    }

    public static HkSyncStatsVO getLastOnboardingStats() {
        return lastOnboardingStats;
    }

    public static HkSyncStatsVO getLastUpdateStats() {
        return lastUpdateStats;
    }

    public static Date getLastOnboardingTime() {
        return lastOnboardingTime;
    }

    public static Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public static Date getLastLifecycleTime() {
        return lastLifecycleTime;
    }

    /**
     * HK员工生命周期：顺序执行 入职 → 变更 → 离职（定时任务/一键同步入口）。
     * 与单独的 syncOnboarding/syncUpdate 互斥，避免并发。
     */
    public void syncAll() {
        synchronized (HkPersonnelSyncTask.class) {
            if (isAnySyncing()) {
                log.warn("HK ERP同步任务正在进行中（lifecycle={}, onboarding={}, update={}），请勿重复操作",
                        lifecycleSyncing, onboardingSyncing, updateSyncing);
                return;
            }
            lifecycleSyncing = true;
        }
        log.info("=== 开始执行: HK ERP员工生命周期同步（入职→变更→离职） ===");
        try {
            // 1. 入职
            try {
                onboardingSyncing = true;
                HkSyncStatsVO onboardStats = syncService.syncNewPersonnel();
                lastOnboardingStats = onboardStats;
                lastOnboardingTime = new Date();
                log.info("--- 生命周期-入职阶段完成: {} ---", onboardStats.getMessage());
            } finally {
                onboardingSyncing = false;
            }

            // 2. 变更 + 离职
            try {
                updateSyncing = true;
                HkSyncStatsVO updateStats = syncService.syncPersonnelUpdate();
                lastUpdateStats = updateStats;
                lastUpdateTime = new Date();
                log.info("--- 生命周期-变更离职阶段完成: {} ---", updateStats.getMessage());
            } finally {
                updateSyncing = false;
            }

            lastLifecycleTime = new Date();
            String summary = String.format("入职[%s] | 变更离职[%s]",
                    lastOnboardingStats != null ? lastOnboardingStats.getMessage() : "未执行",
                    lastUpdateStats != null ? lastUpdateStats.getMessage() : "未执行");
            log.info("=== 完成: HK ERP员工生命周期同步, {} ===", summary);
        } catch (Exception e) {
            log.error("=== 失败: HK ERP员工生命周期同步: {} ===", e.getMessage(), e);
            throw e;
        } finally {
            synchronized (HkPersonnelSyncTask.class) {
                lifecycleSyncing = false;
            }
        }
    }

    /**
     * 入职同步：新员工录入 / 二次入职处理
     */
    public void syncOnboarding() {
        synchronized (HkPersonnelSyncTask.class) {
            if (onboardingSyncing) {
                log.warn("HKERP入职同步正在进行中，请勿重复操作");
                return;
            }
            onboardingSyncing = true;
        }
        log.info("=== 开始执行: HKERP新员工入职同步 ===");
        try {
            HkSyncStatsVO stats = syncService.syncNewPersonnel();
            lastOnboardingStats = stats;
            lastOnboardingTime = new Date();
            log.info("=== 完成: HKERP新员工入职同步, {} ===", stats.getMessage());
        } catch (Exception e) {
            log.error("=== 失败: HKERP新员工入职同步: {} ===", e.getMessage(), e);
            throw e;
        } finally {
            synchronized (HkPersonnelSyncTask.class) {
                onboardingSyncing = false;
            }
        }
    }

    /**
     * 变更 + 离职同步
     */
    public void syncUpdate() {
        synchronized (HkPersonnelSyncTask.class) {
            if (updateSyncing) {
                log.warn("HKERP变更与离职同步正在进行中，请勿重复操作");
                return;
            }
            updateSyncing = true;
        }
        log.info("=== 开始执行: HKERP员工变更与离职同步 ===");
        try {
            HkSyncStatsVO stats = syncService.syncPersonnelUpdate();
            lastUpdateStats = stats;
            lastUpdateTime = new Date();
            log.info("=== 完成: HKERP员工变更与离职同步, {} ===", stats.getMessage());
        } catch (Exception e) {
            log.error("=== 失败: HKERP员工变更与离职同步: {} ===", e.getMessage(), e);
            throw e;
        } finally {
            synchronized (HkPersonnelSyncTask.class) {
                updateSyncing = false;
            }
        }
    }

    /**
     * 单条同步（前端单条触发，同步单个员工到 HK ERP）
     *
     * @param syncType   HK_ONBOARDING / HK_UPDATE / HK_LEAVING
     * @param employeeId IHR员工ID
     * @return null=成功，非空=错误信息
     */
    public String syncSingle(String syncType, String employeeId) {
        return syncService.syncSingle(syncType, employeeId);
    }
}
