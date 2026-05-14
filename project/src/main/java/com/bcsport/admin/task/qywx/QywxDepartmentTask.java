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
    public void sync() {
        log.info("=== 开始执行: 同步企微部门列表 ===");
        try {
            // 先获取数据（无事务，不持有DB连接）
            List<QywxDepartment> deptList = apiClient.getDepartmentList();

            if (deptList == null || deptList.isEmpty()) {
                log.info("=== 完成: 同步企微部门列表, 无数据 ===");
                return;
            }

            // 短事务写库
            doSync(deptList);

            log.info("=== 完成: 同步企微部门列表, 共 {} 个 ===", deptList.size());
        } catch (Exception e) {
            log.error("=== 失败: 同步企微部门列表 ===", e);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void doSync(List<QywxDepartment> deptList) {
        departmentMapper.deleteAll();
        for (int i = 0; i < deptList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, deptList.size());
            departmentMapper.insertBatch(deptList.subList(i, end));
        }
    }
}
