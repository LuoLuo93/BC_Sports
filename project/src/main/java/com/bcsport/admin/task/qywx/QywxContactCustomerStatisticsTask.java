package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.QywxContactCustomerStatistics;
import com.bcsport.admin.qywxmapper.QywxContactCustomerStatisticsMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 企业微信联系客户统计数据同步任务
 */
@Slf4j
@Component("qywxContactCustomerStatisticsTask")
public class QywxContactCustomerStatisticsTask {

    private static final int BATCH_SIZE = 200;

    private static volatile boolean isSyncing = false;

    public static boolean isSyncing() { return isSyncing; }

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxDepartmentMemberMapper departmentMemberMapper;

    @Autowired
    private QywxContactCustomerStatisticsMapper contactCustomerStatisticsMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        synchronized (QywxContactCustomerStatisticsTask.class) {
            if (isSyncing) { log.warn("同步企微联系客户统计正在进行中"); return; }
            isSyncing = true;
        }
        log.info("=== 开始执行: 同步企微联系客户统计 ===");
        long totalStartTime = System.currentTimeMillis();

        try {
            // 获取所有配置了客户联系功能的成员
            List<String> userIds = departmentMemberMapper.selectAllUserIds();
            if (userIds == null || userIds.isEmpty()) {
                log.warn("=== 完成: 无客户联系成员数据 ===");
                return;
            }

            log.info("共 {} 个成员", userIds.size());

            // 计算昨天的时间戳
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date yesterdayStart = calendar.getTime();
            long startTime = yesterdayStart.getTime() / 1000;

            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date yesterdayEnd = calendar.getTime();
            long endTime = yesterdayEnd.getTime() / 1000;

            String yesterdayDateStr = DateUtil.formatDate(yesterdayStart);
            log.info("同步日期: {}", yesterdayDateStr);

            // 增量同步：先删除昨天的数据
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                contactCustomerStatisticsMapper.deleteByDate(yesterdayDateStr);
                return null;
            });

            List<QywxContactCustomerStatistics> allData = new ArrayList<>();

            // 遍历每个成员获取行为数据
            for (String userid : userIds) {
                try {
                    JSONObject result = apiClient.getUserBehaviorData(userid, startTime, endTime);

                    // 如果 userid 不存在（60111），且看起来像手机号，尝试通过手机号解析真正的企微 userid 后重试
                    Integer errcode = result.getInt("errcode");
                    if (errcode != null && errcode == 60111 && userid.matches("^1[3-9]\\d{9}$")) {
                        log.warn("userid {} 返回 60111，尝试通过手机号解析", userid);
                        String realUserId = apiClient.getUserIdByMobile(userid);
                    if (realUserId != null) {
                        log.info("手机号 {} 解析为企微 userid {}，重试获取行为数据", userid, realUserId);
                        result = apiClient.getUserBehaviorData(realUserId, startTime, endTime);
                    } else {
                        log.warn("手机号 {} 在企微中未找到对应用户，跳过", userid);
                    }
                    }

                    JSONArray behaviorData = result.getJSONArray("behavior_data");
                    if (behaviorData != null && behaviorData.size() > 0) {
                        JSONObject data = behaviorData.getJSONObject(0);

                        QywxContactCustomerStatistics stat = new QywxContactCustomerStatistics();
                        stat.setUserid(userid);
                        stat.setStatTime(DateUtil.formatDate(yesterdayStart));
                        stat.setChatCnt(data.getStr("chat_cnt", ""));
                        stat.setMessageCnt(data.getStr("message_cnt", ""));
                        stat.setAvgReplyTime(data.getStr("avg_reply_time", ""));
                        stat.setReplyPercentage(data.getStr("reply_percentage", ""));
                        stat.setNegativeFeedbackCnt(data.getStr("negative_feedback_cnt", ""));
                        stat.setNewApplyCnt(data.getStr("new_apply_cnt", ""));
                        stat.setNewContactCnt(data.getStr("new_contact_cnt", ""));

                        allData.add(stat);
                    }
                } catch (Exception e) {
                    log.error("获取用户 {} 行为数据失败: {}", userid, e.getMessage());
                }
            }

            // 分批插入
            int totalInserted = 0;
            for (int i = 0; i < allData.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allData.size());
                List<QywxContactCustomerStatistics> batch = allData.subList(i, end);
                txTemplate.execute(status -> {
                    contactCustomerStatisticsMapper.insertBatch(batch);
                    return null;
                });
                totalInserted += batch.size();
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 完成: 同步企微联系客户统计, 共 {} 条, 耗时: {} ms ===", totalInserted, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微联系客户统计 ===", e);
            throw new RuntimeException(e);
        } finally {
            synchronized (QywxContactCustomerStatisticsTask.class) { isSyncing = false; }
        }
    }
}
