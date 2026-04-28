package com.bcsport.admin.task.qywx;

import com.bcsport.admin.entity.qywx.QywxDepartmentMember;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 企业微信任务：同步部门成员
 */
@Slf4j
@Component("qywxDepartmentMemberTask")
public class QywxDepartmentMemberTask {

    private static final int BATCH_SIZE = 50;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxDepartmentMemberMapper memberMapper;

    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void sync() {
        log.info("=== Starting: QYWX sync department members ===");
        try {
            List<QywxDepartmentMember> memberList = apiClient.getDepartmentMemberList();

            if (memberList == null || memberList.isEmpty()) {
                log.info("=== Completed: QYWX sync department members, no data ===");
                return;
            }

            memberMapper.deleteAll();

            for (int i = 0; i < memberList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, memberList.size());
                memberMapper.insertBatch(memberList.subList(i, end));
            }

            log.info("=== Completed: QYWX sync department members, total {} ===", memberList.size());
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync department members ===", e);
            throw e;
        }
    }
}
