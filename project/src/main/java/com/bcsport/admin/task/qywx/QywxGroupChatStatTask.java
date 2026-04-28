package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.VxGroupchatYesterday;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberMapper;
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
    private QywxDepartmentMemberMapper departmentMemberMapper;

    @Autowired
    private VxGroupchatYesterdayMapper groupchatYesterdayMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("========================================");
        log.info("=== Starting: QYWX sync group chat statistic ===");
        log.info("========================================");
        long totalStartTime = System.currentTimeMillis();

        try {
            // 获取所有成员
            List<String> userIds = departmentMemberMapper.selectAllUserIds();
            if (userIds == null || userIds.isEmpty()) {
                log.info("No members found, skipping group chat statistic sync");
                return;
            }

            log.info("Found {} members", userIds.size());

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

            log.info("Syncing data for date: {}", yesterdayDateStr);

            // 先清空表
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                groupchatYesterdayMapper.deleteAll();
                return null;
            });

            List<VxGroupchatYesterday> allData = new ArrayList<>();

            // 分批获取数据
            for (int i = 0; i < userIds.size(); i += USER_BATCH_SIZE) {
                int end = Math.min(i + USER_BATCH_SIZE, userIds.size());
                List<String> batchUserIds = userIds.subList(i, end);

                log.info("Processing batch {}-{} of {}", i + 1, end, userIds.size());

                JSONObject result = apiClient.getGroupChatStatistic(batchUserIds, yesterdayTimestamp, yesterdayTimestamp);

                JSONArray items = result.getJSONArray("items");
                if (items != null && items.size() > 0) {
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
                        allData.add(stat);
                    }
                }
            }

            // 批量插入数据
            if (!allData.isEmpty()) {
                final List<VxGroupchatYesterday> dataToInsert = new ArrayList<>(allData);
                txTemplate.execute(status -> {
                    groupchatYesterdayMapper.insertBatch(dataToInsert);
                    return null;
                });
                log.info("Inserted {} group chat statistic records", allData.size());
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== QYWX sync group chat statistic completed, total time: {} ms ===", totalTime);

        } catch (Exception e) {
            log.error("=== QYWX sync group chat statistic failed ===", e);
            throw new RuntimeException(e);
        }
    }

}
