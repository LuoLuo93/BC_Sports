package com.bcsport.admin.task.erp;

import com.bcsport.admin.service.EntityChannelService;
import com.bcsport.admin.service.notify.NotifyManager;
import com.bcsport.admin.service.notify.NotifyMessage;
import com.bcsport.admin.service.notify.NotifyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 实体渠道-店仓名称同步任务
 * <p>
 * 数据流: 伯俊ERP(C_STORE.NAME) → 本地实体渠道表(entity_name)
 * <p>
 * 业务背景：entity_name 在新增/导入时一次性从 ERP 拷贝到本地冗余存储，之后 ERP 改名不会同步过来，
 * 导致列表展示的名称滞后。本任务把本地 store 记录的 entity_name 刷新为 ERP 最新名称。
 * 与手动按钮 {@code POST /api/entity-channel/sync-store-names} 共用同一套 service 逻辑。
 * <p>
 * 推送：任务执行完毕后若有店铺名称被修改，把变更明细(编码/旧名/新名)推到企微群；
 * 手动按钮走 service 不经过本任务，不触发推送。
 */
@Slf4j
@Component("entityChannelStoreNameSyncTask")
public class EntityChannelStoreNameSyncTask {

    /** 推送明细条数上限：企微 markdown 消息 4096 字节，超限截断提示剩余数量 */
    private static final int MAX_DETAIL = 20;

    private static volatile boolean syncing = false;

    @Autowired
    private EntityChannelService entityChannelService;

    @Autowired
    private NotifyManager notifyManager;

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
            pushChangesIfAny(result);
        } catch (Exception e) {
            log.error("实体渠道店仓名称同步失败", e);
            throw e;
        } finally {
            syncing = false;
        }
    }

    /**
     * 有名称修改时推送变更明细到企微群(无修改不打扰)
     */
    @SuppressWarnings("unchecked")
    private void pushChangesIfAny(Map<String, Object> result) {
        List<Map<String, String>> changes = (List<Map<String, String>>) result.get("changes");
        if (changes == null || changes.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("任务名称：ERP-本地店铺名称同步\n");
        sb.append("执行状态：成功\n");
        sb.append("修改店铺：").append(changes.size()).append(" 家\n");
        int shown = Math.min(changes.size(), MAX_DETAIL);
        for (int i = 0; i < shown; i++) {
            Map<String, String> c = changes.get(i);
            String oldName = c.get("oldName");
            sb.append("――――――――\n");
            sb.append("店铺编码：").append(c.get("code")).append("\n");
            sb.append("原名称：").append(oldName == null || oldName.isEmpty() ? "(空)" : oldName).append("\n");
            sb.append("新名称：").append(c.get("newName")).append("\n");
        }
        if (changes.size() > shown) {
            sb.append("\n――――――――\n……其余 ").append(changes.size() - shown).append(" 家详见系统");
        }
        notifyManager.send(NotifyMessage.builder()
                .title("店仓名称同步修改通知(" + changes.size() + "家)")
                .type(NotifyType.INFO)
                .content(sb.toString())
                .build());
    }
}
