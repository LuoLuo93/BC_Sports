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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 企业微信联系客户统计数据同步任务
 */
@Slf4j
@Component("qywxContactCustomerStatisticsTask")
public class QywxContactCustomerStatisticsTask {

    private static final int BATCH_SIZE = 200;

    /** 企微 get_user_behavior_data 单次允许的 userid 数量上限(逗号串) */
    private static final int WECHAT_API_BATCH_USERS = 100;

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

            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            List<QywxContactCustomerStatistics> allData = new ArrayList<>();

            // 企微接口支持 userid 逗号串批量(≤100/次)：先批量拉取，未返回的成员再逐个回退——
            // 回退路径保留 60111 手机号救援逻辑(批量响应里无效账号只是缺席，没有 60111 可判)
            Set<String> returnedUserids = new HashSet<>();
            for (int i = 0; i < userIds.size(); i += WECHAT_API_BATCH_USERS) {
                int end = Math.min(i + WECHAT_API_BATCH_USERS, userIds.size());
                List<String> chunk = userIds.subList(i, end);
                try {
                    JSONObject result = apiClient.getUserBehaviorDataBatch(chunk, startTime, endTime);
                    JSONArray behaviorData = result.getJSONArray("behavior_data");
                    if (behaviorData != null) {
                        for (int j = 0; j < behaviorData.size(); j++) {
                            JSONObject data = behaviorData.getJSONObject(j);
                            String uid = data.getStr("userid", "");
                            if (uid.isEmpty()) continue;
                            returnedUserids.add(uid);
                            allData.add(buildStat(uid, data, yesterdayStart));
                        }
                    }
                } catch (Exception e) {
                    // 不在此逐个回退：该区间成员都未进 returnedUserids，
                    // 由下方缺席回退循环统一单人重试(含60111救援)，避免重复拉取
                    log.error("批量获取行为数据失败(区间 {}-{})，该区间成员转入缺席回退: {}", i, end, e.getMessage());
                }
            }

            // 批量响应里缺席的成员：可能是账号无效(如存的是手机号)或当日无数据，逐个确认
            for (String userid : userIds) {
                if (returnedUserids.contains(userid)) continue;
                QywxContactCustomerStatistics stat = fetchSingleWithRescue(
                        userid, startTime, endTime, yesterdayStart);
                if (stat != null) allData.add(stat);
            }

            // 拉取完成后再"删昨日旧数据+插新数据"，同一事务：中途拉取失败不会丢昨日已有数据
            txTemplate.execute(status -> {
                contactCustomerStatisticsMapper.deleteByDate(yesterdayDateStr);
                for (int i = 0; i < allData.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allData.size());
                    contactCustomerStatisticsMapper.insertBatch(allData.subList(i, end));
                }
                return null;
            });
            int totalInserted = allData.size();

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 完成: 同步企微联系客户统计, 共 {} 条, 耗时: {} ms ===", totalInserted, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微联系客户统计 ===", e);
            throw new RuntimeException(e);
        } finally {
            synchronized (QywxContactCustomerStatisticsTask.class) { isSyncing = false; }
        }
    }

    private QywxContactCustomerStatistics buildStat(String userid, JSONObject data, Date statDate) {
        QywxContactCustomerStatistics stat = new QywxContactCustomerStatistics();
        stat.setUserid(userid);
        stat.setStatTime(DateUtil.formatDate(statDate));
        stat.setChatCnt(data.getStr("chat_cnt", ""));
        stat.setMessageCnt(data.getStr("message_cnt", ""));
        stat.setAvgReplyTime(data.getStr("avg_reply_time", ""));
        stat.setReplyPercentage(data.getStr("reply_percentage", ""));
        stat.setNegativeFeedbackCnt(data.getStr("negative_feedback_cnt", ""));
        stat.setNewApplyCnt(data.getStr("new_apply_cnt", ""));
        stat.setNewContactCnt(data.getStr("new_contact_cnt", ""));
        return stat;
    }

    /**
     * 单人拉取（批量响应中缺席成员的回退路径）：保留 60111 手机号救援逻辑——
     * 批量响应里无效账号只是缺席没有 errcode，无法据此判定，必须逐个确认。
     *
     * @return 当日无数据或失败返回 null
     */
    private QywxContactCustomerStatistics fetchSingleWithRescue(
            String userid, long startTime, long endTime, Date statDate) {
        try {
            JSONObject result = apiClient.getUserBehaviorData(userid, startTime, endTime);

            // userid 不存在（60111）且形如手机号 → 解析真正的企微 userid 后重试
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
                return buildStat(userid, behaviorData.getJSONObject(0), statDate);
            }
        } catch (Exception e) {
            log.error("获取用户 {} 行为数据失败: {}", userid, e.getMessage());
        }
        return null;
    }
}
