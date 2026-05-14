package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.QywxMoment;
import com.bcsport.admin.qywxmapper.QywxMomentMapper;
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
 * 企业微信朋友圈同步任务
 */
@Slf4j
@Component("qywxMomentTask")
public class QywxMomentTask {

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxMomentMapper momentMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("=== 开始执行: 同步企微朋友圈 ===");
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

            // 增量同步：先删除昨天的数据
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                momentMapper.deleteYesterdayData();
                return null;
            });

            int totalInserted = 0;
            String cursor = "";

            // 循环获取所有数据（支持游标翻页），每批获取后立即插入
            do {
                JSONObject result = apiClient.getMomentList(yesterdayStartTimestamp, yesterdayEndTimestamp, cursor);

                JSONArray momentList = result.getJSONArray("moment_list");
                if (momentList != null && momentList.size() > 0) {
                    final List<QywxMoment> batchMoments = new ArrayList<>();

                    for (int i = 0; i < momentList.size(); i++) {
                        JSONObject msgItem = momentList.getJSONObject(i);
                        QywxMoment moment = new QywxMoment();
                        moment.setMomentId(msgItem.getStr("moment_id"));
                        moment.setCreator(msgItem.getStr("creator", ""));

                        String createTime = msgItem.getStr("create_time");
                        if (createTime != null) {
                            try {
                                moment.setCreateTime(DateUtil.formatDateTime(new Date(Long.parseLong(createTime) * 1000)));
                            } catch (Exception e) {
                                moment.setCreateTime(createTime);
                            }
                        }
                        moment.setCreateType(msgItem.getStr("create_type"));

                        JSONObject textObj = msgItem.getJSONObject("text");
                        if (textObj != null) {
                            moment.setContent(textObj.getStr("content"));
                        }

                        batchMoments.add(moment);
                    }

                    // 立即插入这一批数据
                    if (!batchMoments.isEmpty()) {
                        txTemplate.execute(status -> {
                            momentMapper.insertBatch(batchMoments);
                            return null;
                        });
                        totalInserted += batchMoments.size();
                    }
                }

                cursor = result.getStr("next_cursor", "");

            } while (cursor != null && cursor.length() > 0);

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 完成: 同步企微朋友圈, 共 {} 条, 耗时: {} ms ===", totalInserted, totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微朋友圈 ===", e);
            throw new RuntimeException(e);
        }
    }

}
