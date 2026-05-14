package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.VxGroupchatYesterday;
import com.bcsport.admin.qywxmapper.QywxFollowUserMapper;
import com.bcsport.admin.qywxmapper.VxGroupchatYesterdayMapper;
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
 * 企业微信群聊统计数据同步任务
 */
@Slf4j
@Component("qywxGroupChatStatTask")
public class QywxGroupChatStatTask {

    private static final int USER_BATCH_SIZE = 100;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxFollowUserMapper followUserMapper;

    @Autowired
    private VxGroupchatYesterdayMapper groupchatYesterdayMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("=== 开始执行: 同步企微群聊统计 ===");
        long totalStartTime = System.currentTimeMillis();

        try {
            // 获取所有配置了客户联系功能的成员
            List<String> userIds = followUserMapper.selectAllUserIds();
            if (userIds == null || userIds.isEmpty()) {
                log.warn("=== 完成: 无客户联系成员数据 ===");
                return;
            }

            log.info("共 {} 个成员", userIds.size());

            // 计算昨天的时间戳（开始和结束都是昨天0点）
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date yesterday = calendar.getTime();
            long yesterdayTimestamp = yesterday.getTime() / 1000;
            String yesterdayDateStr = DateUtil.formatDate(yesterday);

            log.info("同步日期: {}", yesterdayDateStr);

            // 增量同步：只删除昨天的数据（避免重复插入）
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                groupchatYesterdayMapper.deleteByDate(yesterdayDateStr);
                return null;
            });

            int totalInserted = 0;

            // 分批获取数据，每批获取后立即插入
            for (int i = 0; i < userIds.size(); i += USER_BATCH_SIZE) {
                int end = Math.min(i + USER_BATCH_SIZE, userIds.size());
                List<String> batchUserIds = userIds.subList(i, end);

                JSONObject result = apiClient.getGroupChatStatistic(batchUserIds, yesterdayTimestamp, yesterdayTimestamp);

                JSONArray items = result.getJSONArray("items");
                if (items != null && items.size() > 0) {
                    final List<VxGroupchatYesterday> batchData = new ArrayList<>();

                    for (int j = 0; j < items.size(); j++) {
                        JSONObject item = items.getJSONObject(j);
                        VxGroupchatYesterday stat = new VxGroupchatYesterday();
                        stat.setOwner(item.getStr("owner"));
                        JSONObject data = item.getJSONObject("data");
                        if (data != null) {
                            stat.setNewChatCnt(data.getStr("new_chat_cnt"));
                            stat.setChatTotal(data.getStr("chat_total"));
                            stat.setChatHasMsg(data.getStr("chat_has_msg"));
                            stat.setNewMemberCnt(data.getStr("new_member_cnt"));
                            stat.setMemberTotal(data.getStr("member_total"));
                            stat.setMemberHasMsg(data.getStr("member_has_msg"));
                            stat.setMsgTotal(data.getStr("msg_total"));
                        }
                        stat.setStarttime(yesterdayDateStr);
                        batchData.add(stat);
                    }

                    // 立即插入这一批数据
                    if (!batchData.isEmpty()) {
                        txTemplate.execute(status -> {
                            groupchatYesterdayMapper.insertBatch(batchData);
                            return null;
                        });
                        totalInserted += batchData.size();
                    }
                }
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 完成: 同步企微群聊统计, 共 {} 条, 耗时: {} ms ===", totalInserted, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微群聊统计 ===", e);
            throw new RuntimeException(e);
        }
    }

}
