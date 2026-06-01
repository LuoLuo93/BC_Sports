package com.bcsport.admin.task.erp;

import com.bcsport.admin.service.ErpEmployeeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("erpEmployeeSyncTask")
public class ErpEmployeeSyncTask {

    private static volatile boolean syncing = false;

    @Autowired
    private ErpEmployeeSyncService erpSyncService;

    public static boolean isSyncing() {
        return syncing;
    }

    /**
     * 同步所有待同步人员到ERP
     * TODO: 实现具体ERP API调用逻辑
     */
    public void syncAll() {
        synchronized (ErpEmployeeSyncTask.class) {
            if (syncing) {
                log.warn("ERP人员同步正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: ERP人员同步 ===");

        try {
            // TODO: 实现以下逻辑
            // 1. 从 erp_employee_sync_status 查询 sync_status=0 的记录
            // 2. 根据 sync_type 调用对应的ERP API
            // 3. 根据结果调用 erpSyncService.markSyncSuccess/Failed/Skipped
        } finally {
            synchronized (ErpEmployeeSyncTask.class) {
                syncing = false;
            }
        }

        log.info("=== ERP人员同步完成 (skeleton) ===");
    }

    /**
     * 同步单个人员到ERP
     * TODO: 实现具体ERP API调用逻辑
     */
    public String syncSingle(String syncType, String employeeId) {
        log.info("手动同步单个人员到ERP, syncType={}, employeeId={}", syncType, employeeId);

        // TODO: 实现以下逻辑
        // 1. 根据 employeeId 查询员工详细信息
        // 2. 调用ERP API同步
        // 3. 返回 null 表示成功，错误信息表示失败

        return null;
    }
}
