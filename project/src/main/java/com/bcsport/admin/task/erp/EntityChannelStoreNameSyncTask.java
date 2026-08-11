package com.bcsport.admin.task.erp;

import com.bcsport.admin.service.EntityChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 实体渠道-店仓名称同步任务
 * <p>
 * 数据流: 伯俊ERP(C_STORE.NAME) → 本地实体渠道表(entity_name)
 * <p>
 * 业务背景：entity_name 在新增/导入时一次性从 ERP 拷贝到本地冗余存储，之后 ERP 改名不会同步过来，
 * 导致列表展示的名称滞后。本任务把本地 store 记录的 entity_name 刷新为 ERP 最新名称。
 * 与手动按钮 {@code POST /api/entity-channel/sync-store-names} 共用同一套 service 逻辑。
 */
@Slf4j
@Component("entityChannelStoreNameSyncTask")
public class EntityChannelStoreNameSyncTask {

    private static volatile boolean syncing = false;

    @Autowired
    private EntityChannelService entityChannelService;

    public static boolean isSyncing() {
        return syncing;
    }

    /**
     * 同步本地店仓名称为伯俊 ERP 的最新名称。
     * 定时任务入口（无参，由 ScheduleConfig 反射调用）。
     */
    public void syncStoreNames() {
        synchronized (EntityChannelStoreNameSyncTask.class) {
            if (syncing) {
                log.warn("实体渠道店仓名称同步任务正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: 实体渠道店仓名称同步(从伯俊ERP) ===");
        try {
            Map<String, Object> result = entityChannelService.syncStoreNames();
            log.info("实体渠道店仓名称同步完成：总计 {} 条，更新 {} 条，未变化 {} 条，ERP中不存在 {} 条",
                    result.get("total"), result.get("synced"), result.get("unchanged"), result.get("notInErp"));
        } catch (Exception e) {
            log.error("实体渠道店仓名称同步失败", e);
            throw e;
        } finally {
            syncing = false;
        }
    }
}
