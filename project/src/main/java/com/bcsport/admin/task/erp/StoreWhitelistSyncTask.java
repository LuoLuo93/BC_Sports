package com.bcsport.admin.task.erp;

import com.bcsport.admin.service.StoreWhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自提店铺白名单同步任务
 * <p>
 * 数据流: 伯俊ERP(C_STORE 中扩展属性8=自提店铺的店仓) → 本地 BC_SPORTS_BI_STORE_WHITELIST(source=AUTO)
 * <p>
 * 规则：ERP 有该属性 → 新增 AUTO 行/名称变化时更新；ERP 摘除属性 → 软删对应 AUTO 行；
 * 手工录入行(source=MANUAL)永不改动。与白名单页面"从ERP同步"按钮共用同一套 service 逻辑。
 */
@Slf4j
@Component("storeWhitelistSyncTask")
public class StoreWhitelistSyncTask {

    private static volatile boolean syncing = false;

    @Autowired
    private StoreWhitelistService storeWhitelistService;

    public static boolean isSyncing() {
        return syncing;
    }

    /**
     * 同步自提店铺到白名单。
     * 定时任务入口（无参，由 ScheduleConfig 反射调用）；属性值走系统配置 store.whitelist.attrib8Id(默认7582)。
     */
    public void sync() {
        synchronized (StoreWhitelistSyncTask.class) {
            if (syncing) {
                log.warn("自提店铺白名单同步任务正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: 自提店铺白名单同步(从伯俊ERP C_STORE 自提属性) ===");
        try {
            Map<String, Object> result = storeWhitelistService.syncFromErp(null);
            log.info("自提店铺白名单同步完成：ERP共 {} 家(attrib={}), 新增 {} 家, 改名 {} 家, 无变化 {} 家, "
                            + "手工行跳过 {} 家, 移出 {} 家",
                    result.get("total"), result.get("attrib"), result.get("inserted"), result.get("updated"),
                    result.get("unchanged"), result.get("manualSkip"), result.get("removed"));
        } catch (Exception e) {
            log.error("自提店铺白名单同步失败", e);
            throw e;
        } finally {
            syncing = false;
        }
    }
}
