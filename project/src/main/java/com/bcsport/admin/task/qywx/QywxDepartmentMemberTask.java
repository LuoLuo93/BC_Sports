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

    public void sync() {
        log.info("=== 开始执行: 同步企微部门成员 ===");
        try {
            List<QywxDepartmentMember> memberList = apiClient.getDepartmentMemberList();

            if (memberList == null || memberList.isEmpty()) {
                log.info("=== 完成: 同步企微部门成员, 无数据 ===");
                return;
            }

            doSync(memberList);

            log.info("=== 完成: 同步企微部门成员, 共 {} 人 ===", memberList.size());
        } catch (Exception e) {
            log.error("=== 失败: 同步企微部门成员 ===", e);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void doSync(List<QywxDepartmentMember> memberList) {
        memberMapper.deleteAll();
        for (int i = 0; i < memberList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, memberList.size());
            memberMapper.insertBatch(memberList.subList(i, end));
        }
    }
}
