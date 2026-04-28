package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsExternalContact;
import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsFollowInfo;
import com.bcsport.admin.qywxmapper.VxCustomerlistdetailsExternalContactMapper;
import com.bcsport.admin.qywxmapper.VxCustomerlistdetailsFollowInfoMapper;
import com.bcsport.admin.qywxmapper.QywxFollowUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业微信任务：同步客户详情
 * 每个线程：请求API一页 → 解析 → 写库 → 请求下一页 → 写库 ...
 */
@Slf4j
@Component("qywxCustomerDetailTask")
public class QywxCustomerDetailTask {

    private static final int BATCH_SIZE = 100;
    private static final int USER_BATCH_SIZE = 100;
    private static final int CONCURRENT_TASKS = 3;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxFollowUserMapper followUserMapper;

    @Autowired
    private VxCustomerlistdetailsExternalContactMapper externalContactMapper;

    @Autowired
    private VxCustomerlistdetailsFollowInfoMapper followInfoMapper;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        log.info("========================================");
        log.info("=== Starting: QYWX sync customer detail ===");
        log.info("========================================");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 清空表
            log.info("Clearing existing data...");
            new TransactionTemplate(transactionManager).execute(status -> {
                externalContactMapper.deleteAll();
                followInfoMapper.deleteAll();
                return null;
            });

            // 2. 获取成员列表
            List<String> followUserList = followUserMapper.selectAllUserIds();
            if (followUserList == null || followUserList.isEmpty()) {
                log.info("=== Completed: no follow users ===");
                return;
            }

            List<String> validUserList = new ArrayList<>();
            for (String userId : followUserList) {
                if (userId != null && !userId.trim().isEmpty()) {
                    validUserList.add(userId.trim());
                }
            }
            if (validUserList.isEmpty()) {
                log.warn("=== Completed: no valid userid ===");
                return;
            }
            log.info("Found {} valid follow users", validUserList.size());

            // 3. 多线程并行
            syncCustomerDetails(validUserList);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== Completed: QYWX sync customer detail, time: {} ms ===", totalTime);

        } catch (Exception e) {
            log.error("=== Failed: QYWX sync customer detail ===", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 多线程并行：每个线程请求API一页就写库一页，再请求下一页
     */
    private void syncCustomerDetails(List<String> followUserList) {
        log.info("开始并行获取客户详情...");
        long startTime = System.currentTimeMillis();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger totalContacts = new AtomicInteger(0);
        AtomicInteger totalFollowInfos = new AtomicInteger(0);

        Set<String> globalProcessedExternalUserIds = Collections.synchronizedSet(new HashSet<>());

        int totalBatches = (followUserList.size() + USER_BATCH_SIZE - 1) / USER_BATCH_SIZE;
        Semaphore semaphore = new Semaphore(CONCURRENT_TASKS);
        CountDownLatch totalLatch = new CountDownLatch(totalBatches);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (int i = 0; i < followUserList.size(); i += USER_BATCH_SIZE) {
            int end = Math.min(i + USER_BATCH_SIZE, followUserList.size());
            final List<String> batchUserIds = new ArrayList<>(followUserList.subList(i, end));
            final int currentBatch = i / USER_BATCH_SIZE;

            taskThreadPool.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        log.info("开始处理批次 {}/{}", currentBatch + 1, totalBatches);

                        // 边翻页边写库
                        fetchPageAndWrite(batchUserIds, txTemplate,
                                globalProcessedExternalUserIds, totalContacts, totalFollowInfos);

                        successCount.incrementAndGet();
                        log.info("完成处理批次 {}/{}", currentBatch + 1, totalBatches);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("处理批次失败 {}/{}", currentBatch + 1, totalBatches, e);
                    failCount.incrementAndGet();
                } finally {
                    totalLatch.countDown();
                }
            });
        }

        try {
            boolean completed = totalLatch.await(30, TimeUnit.MINUTES);
            if (!completed) {
                log.warn("等待超时，部分任务可能未完成");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("并行获取客户详情完成，成功: {}, 失败: {}, contacts: {}, followInfos: {}, 耗时: {} ms",
                successCount.get(), failCount.get(), totalContacts.get(), totalFollowInfos.get(),
                System.currentTimeMillis() - startTime);
    }

    /**
     * 请求API一页数据 → 解析 → 写库 → 请求下一页 → 写库 ... 循环直到没有下一页
     */
    private void fetchPageAndWrite(List<String> userIds, TransactionTemplate txTemplate,
                                    Set<String> globalProcessedExternalUserIds,
                                    AtomicInteger totalContacts, AtomicInteger totalFollowInfos) {
        List<VxCustomerlistdetailsExternalContact> contactWriteBatch = new ArrayList<>();
        List<VxCustomerlistdetailsFollowInfo> followInfoWriteBatch = new ArrayList<>();

        String cursor = "";
        do {
            // 1. 请求一页
            JSONObject result = apiClient.batchGetByUser(userIds, cursor);
            if (result == null) break;

            // 2. 解析这一页
            JSONArray externalContactList = result.getJSONArray("external_contact_list");
            if (externalContactList != null && externalContactList.size() > 0) {
                for (int j = 0; j < externalContactList.size(); j++) {
                    JSONObject item = externalContactList.getJSONObject(j);

                    JSONObject externalContact = item.getJSONObject("external_contact");
                    if (externalContact == null) continue;

                    String externalUserid = externalContact.getStr("external_userid", "");
                    if (externalUserid.length() == 0) continue;

                    // 外部联系人（全局去重）
                    if (globalProcessedExternalUserIds.add(externalUserid)) {
                        VxCustomerlistdetailsExternalContact contact = new VxCustomerlistdetailsExternalContact();
                        contact.setExternalUserid(externalUserid);
                        contact.setName(externalContact.getStr("name", ""));
                        contact.setType(externalContact.getStr("type", ""));
                        contact.setGender(externalContact.getStr("gender", ""));
                        contact.setUnionid(externalContact.getStr("unionid", ""));
                        contact.setCorpName(externalContact.getStr("corp_name", ""));
                        contactWriteBatch.add(contact);
                        totalContacts.incrementAndGet();

                        // 达到批次大小就写库
                        if (contactWriteBatch.size() >= BATCH_SIZE) {
                            final List<VxCustomerlistdetailsExternalContact> toWrite = new ArrayList<>(contactWriteBatch);
                            txTemplate.execute(status -> {
                                externalContactMapper.insertBatch(toWrite);
                                return null;
                            });
                            contactWriteBatch.clear();
                        }
                    }

                    // 跟进信息（不去重）
                    JSONObject followInfo = item.getJSONObject("follow_info");
                    if (followInfo != null) {
                        VxCustomerlistdetailsFollowInfo info = new VxCustomerlistdetailsFollowInfo();
                        info.setUserid(followInfo.getStr("userid", ""));
                        info.setRemark(followInfo.getStr("remark", ""));
                        info.setDescription(followInfo.getStr("description", ""));
                        info.setCreatetime(followInfo.getStr("createtime", ""));
                        info.setAddWay(followInfo.getStr("add_way", ""));
                        info.setOperUserid(followInfo.getStr("oper_userid", ""));
                        info.setExternalUserid(externalUserid);
                        followInfoWriteBatch.add(info);
                        totalFollowInfos.incrementAndGet();

                        // 达到批次大小就写库
                        if (followInfoWriteBatch.size() >= BATCH_SIZE) {
                            final List<VxCustomerlistdetailsFollowInfo> toWrite = new ArrayList<>(followInfoWriteBatch);
                            txTemplate.execute(status -> {
                                followInfoMapper.insertBatch(toWrite);
                                return null;
                            });
                            followInfoWriteBatch.clear();
                        }
                    }
                }
            }

            // 3. 取下一页cursor
            cursor = result.getStr("next_cursor", "");

        } while (cursor != null && cursor.length() > 0);

        // 写入剩余数据
        if (!contactWriteBatch.isEmpty()) {
            final List<VxCustomerlistdetailsExternalContact> toWrite = new ArrayList<>(contactWriteBatch);
            txTemplate.execute(status -> {
                externalContactMapper.insertBatch(toWrite);
                return null;
            });
        }
        if (!followInfoWriteBatch.isEmpty()) {
            final List<VxCustomerlistdetailsFollowInfo> toWrite = new ArrayList<>(followInfoWriteBatch);
            txTemplate.execute(status -> {
                followInfoMapper.insertBatch(toWrite);
                return null;
            });
        }
    }
}
