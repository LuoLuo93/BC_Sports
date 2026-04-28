package com.bcsport.admin.task.qywx;

import com.bcsport.admin.entity.qywx.QywxDepartment;
import com.bcsport.admin.qywxmapper.QywxDepartmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 企业微信任务：同步部门列表
 */
@Slf4j
@Component("qywxDepartmentTask")
public class QywxDepartmentTask {

    private static final int BATCH_SIZE = 50;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxDepartmentMapper departmentMapper;

    /**
     * 同步部门列表
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void sync() {
        log.info("=== Starting: QYWX sync department list ===");
        try {
            List<QywxDepartment> deptList = apiClient.getDepartmentList();

            if (deptList == null || deptList.isEmpty()) {
                log.info("=== Completed: QYWX sync department list, no data ===");
                return;
            }

            // 先删除旧数据
            departmentMapper.deleteAll();

            // 分批插入
            for (int i = 0; i < deptList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, deptList.size());
                departmentMapper.insertBatch(deptList.subList(i, end));
            }

            log.info("=== Completed: QYWX sync department list, total {} ===", deptList.size());
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync department list ===", e);
            throw e;
        }
    }
}
