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
        log.info("=== 开始执行: 同步企微群发消息 ===");
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

            log.info("同步范围: {} ~ {}",
                     DateUtil.formatDateTime(yesterdayStart),
                     DateUtil.formatDateTime(yesterdayEnd));

            // 先清空昨天的数据
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                massMessageMapper.deleteYesterdayData();
                return null;
            });

            int totalInserted = 0;
            String cursor = "";

            // 循环获取所有数据（支持游标翻页），每批获取后立即插入
            do {
                JSONObject result = apiClient.getMassMessageList(yesterdayStartTimestamp, yesterdayEndTimestamp, cursor, "single");

                JSONArray groupMsgList = result.getJSONArray("group_msg_list");
                if (groupMsgList != null && groupMsgList.size() > 0) {
                    final List<VxMassMessage> batchMessages = new ArrayList<>();

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

                        batchMessages.add(msg);
                    }

                    // 立即插入这一批数据
                    if (!batchMessages.isEmpty()) {
                        txTemplate.execute(status -> {
                            massMessageMapper.insertBatch(batchMessages);
                            return null;
                        });
                        totalInserted += batchMessages.size();
                    }
                }

                cursor = result.getStr("next_cursor", "");

            } while (cursor != null && cursor.length() > 0);

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 完成: 同步企微群发消息, 共 {} 条, 耗时: {} ms ===", totalInserted, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微群发消息 ===", e);
            throw new RuntimeException(e);
        }
    }

}
