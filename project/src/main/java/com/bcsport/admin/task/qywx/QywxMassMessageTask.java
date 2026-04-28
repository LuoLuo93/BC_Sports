package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.VxMassMessage;
import com.bcsport.admin.qywxmapper.VxMassMessageMapper;
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
 * 企业微信群发消息记录同步任务
 */
@Slf4j
@Component("qywxMassMessageTask")
public class QywxMassMessageTask {

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private VxMassMessageMapper massMessageMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("========================================");
        log.info("=== Starting: QYWX sync mass message ===");
        log.info("========================================");
        long totalStartTime = System.currentTimeMillis();

        try {
            // 计算昨天的开始和结束时间戳
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date yesterdayStart = calendar.getTime();
            long yesterdayStartTimestamp = yesterdayStart.getTime() / 1000;

            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date yesterdayEnd = calendar.getTime();
            long yesterdayEndTimestamp = yesterdayEnd.getTime() / 1000;

            log.info("Syncing mass messages from {} to {}",
                     DateUtil.formatDateTime(yesterdayStart),
                     DateUtil.formatDateTime(yesterdayEnd));

            // 先清空昨天的数据
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                massMessageMapper.deleteYesterdayData();
                return null;
            });

            List<VxMassMessage> allMessages = new ArrayList<>();
            String cursor = "";

            // 循环获取所有数据（支持游标翻页）
            do {
                JSONObject result = apiClient.getMassMessageList(yesterdayStartTimestamp, yesterdayEndTimestamp, cursor, "single");

                JSONArray groupMsgList = result.getJSONArray("group_msg_list");
                if (groupMsgList != null && groupMsgList.size() > 0) {
                    for (int i = 0; i < groupMsgList.size(); i++) {
                        JSONObject msgItem = groupMsgList.getJSONObject(i);
                        VxMassMessage msg = new VxMassMessage();
                        msg.setMsgid(msgItem.getStr("msgid"));
                        msg.setCreateType(msgItem.getStr("create_type"));
                        msg.setCreator(msgItem.getStr("creator"));

                        Long createTime = msgItem.getLong("create_time");
                        if (createTime != null) {
                            msg.setCreateTime(DateUtil.formatDateTime(new Date(createTime * 1000)));
                        }

                        JSONObject textObj = msgItem.getJSONObject("text");
                        if (textObj != null) {
                            msg.setContent(textObj.getStr("content"));
                        }

                        allMessages.add(msg);
                    }
                }

                cursor = result.getStr("next_cursor", "");
                log.info("Fetched {} messages, next cursor: {}", groupMsgList != null ? groupMsgList.size() : 0, cursor);

            } while (cursor != null && cursor.length() > 0);

            // 批量插入数据
            if (!allMessages.isEmpty()) {
                final List<VxMassMessage> messagesToInsert = new ArrayList<>(allMessages);
                txTemplate.execute(status -> {
                    massMessageMapper.insertBatch(messagesToInsert);
                    return null;
                });
                log.info("Inserted {} mass message records", allMessages.size());
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== QYWX sync mass message completed, total time: {} ms ===", totalTime);

        } catch (Exception e) {
            log.error("=== QYWX sync mass message failed ===", e);
            throw new RuntimeException(e);
        }
    }

}
