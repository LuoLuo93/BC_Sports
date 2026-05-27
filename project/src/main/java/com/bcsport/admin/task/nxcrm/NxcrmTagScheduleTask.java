package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.service.nxcrm.NxcrmTagTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("nxcrmTagScheduleTask")
public class NxcrmTagScheduleTask {

    @Resource
    private NxcrmTagTaskService tagTaskService;

    public void fillShopId() {
        log.info("=== 开始执行: 南讯CRM填充shopId ===");
        try {
            tagTaskService.fillAllPendingShopId();
            log.info("=== 完成执行: 南讯CRM填充shopId ===");
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM填充shopId ===", e);
            throw e;
        }
    }
}
