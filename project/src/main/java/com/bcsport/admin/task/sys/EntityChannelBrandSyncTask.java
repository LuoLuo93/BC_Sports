package com.bcsport.admin.task.sys;

import com.bcsport.admin.service.EntityChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 实体渠道-店铺品牌同步任务
 * <p>
 * 数据流: 数仓(销售 ODS_SALES_MAIN + 库存 DWD_STOCK_DAILY) → distinct(店铺+品牌)
 *         → 匹配本地 bc_sports_sys_brand(匹配不到的跳过)
 *         → 对比 bc_sports_sys_entity_channel 缺失的 (external_id, brand_id) 自动新增。
 * <p>
 * 与手动按钮 {@code POST /api/entity-channel/sync-store-brands} 共用同一套 service 逻辑。
 */
@Slf4j
@Component("entityChannelBrandSyncTask")
public class EntityChannelBrandSyncTask {

    private static volatile boolean syncing = false;

    @Autowired
    private EntityChannelService entityChannelService;

    public static boolean isSyncing() {
        return syncing;
    }

    /**
     * 从数仓同步店铺+品牌到实体渠道配置。
     * 定时任务入口（无参，由 ScheduleConfig 反射调用）。
     */
    public void syncStoreBrands() {
        synchronized (EntityChannelBrandSyncTask.class) {
            if (syncing) {
                log.warn("实体渠道店铺品牌同步任务正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: 实体渠道店铺品牌同步(从数仓) ===");
        try {
            Map<String, Object> result = entityChannelService.syncStoreBrands();
            log.info("实体渠道店铺品牌同步完成：总计 {} 条，新增 {} 条，复活 {} 条，已存在 {} 条，品牌未匹配 {} 条 [{}]",
                    result.get("total"), result.get("inserted"), result.get("revived"),
                    result.get("existing"), result.get("brandUnmatched"), result.get("unmatchedBrands"));
        } catch (Exception e) {
            log.error("实体渠道店铺品牌同步失败", e);
            throw e;
        } finally {
            syncing = false;
        }
    }
}
