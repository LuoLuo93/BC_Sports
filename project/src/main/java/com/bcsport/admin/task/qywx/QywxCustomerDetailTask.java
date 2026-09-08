package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
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
        log.info("=== 开始执行: 同步客户详情 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 只清空影子表(清掉上轮残留)。主表不动：拉取中途失败时旧数据完整保留，全部成功后才切换
            new TransactionTemplate(transactionManager).execute(status -> {
                externalContactMapper.clearStg();
                followInfoMapper.clearStg();
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
            log.info("共 {} 个成员需要同步客户详情", validUserList.size());

            // 3. 多线程并行拉取，写入影子表
            int[] result = syncCustomerDetails(validUserList);
            int failedBatches = result[1];
            int totalBatches = (validUserList.size() + USER_BATCH_SIZE - 1) / USER_BATCH_SIZE;

            // 4. 失败率超10%(典型如token失效导致全部批次失败)时放弃切换，保留主表旧数据
            if (failedBatches * 10 > totalBatches) {
                throw new IllegalStateException(String.format(
                        "同步客户详情失败率过高(失败批次 %d/%d)，保留主表旧数据不切换", failedBatches, totalBatches));
            }

            // 5. 原子切换：主表清空+影子表回填+清影子表，同一事务，任一步失败整体回滚
            new TransactionTemplate(transactionManager).execute(status -> {
                externalContactMapper.deleteAll();
                externalContactMapper.copyFromStg();
                externalContactMapper.clearStg();
                followInfoMapper.deleteAll();
                followInfoMapper.copyFromStg();
                followInfoMapper.clearStg();
                return null;
            });

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== 完成: 同步客户详情, 耗时: {} ms ===", totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步客户详情 ===", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 多线程并行：每个线程请求API一页就写影子表一页，再请求下一页
     *
     * @return [成功批次数, 失败批次数]
     */
    private int[] syncCustomerDetails(List<String> followUserList) {
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
                        fetchPageAndWrite(batchUserIds, txTemplate,
                                globalProcessedExternalUserIds, totalContacts, totalFollowInfos);

                        successCount.incrementAndGet();
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
                // 超时=部分批次未执行完，影子表数据不完整，必须放弃切换保住主表旧数据
                throw new IllegalStateException("等待客户详情批次超时(30分钟)，本轮放弃切换");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("客户详情同步被中断，本轮放弃切换", e);
        }

        log.info("客户详情同步完成, 成功: {}, 失败: {}, contacts: {}, followInfos: {}, 耗时: {} ms",
                successCount.get(), failCount.get(), totalContacts.get(), totalFollowInfos.get(),
                System.currentTimeMillis() - startTime);
        return new int[]{successCount.get(), failCount.get()};
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
                                externalContactMapper.insertBatchStg(toWrite);
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
                        String ct = followInfo.getStr("createtime", "");
                        if (ct != null && !ct.isEmpty()) {
                            try {
                                info.setCreatetime(DateUtil.formatDateTime(new Date(Long.parseLong(ct) * 1000)));
                            } catch (Exception e) {
                                info.setCreatetime(ct);
                            }
                        }
                        info.setAddWay(followInfo.getStr("add_way", ""));
                        info.setOperUserid(followInfo.getStr("oper_userid", ""));
                        info.setExternalUserid(externalUserid);
                        followInfoWriteBatch.add(info);
                        totalFollowInfos.incrementAndGet();

                        // 达到批次大小就写库
                        if (followInfoWriteBatch.size() >= BATCH_SIZE) {
                            final List<VxCustomerlistdetailsFollowInfo> toWrite = new ArrayList<>(followInfoWriteBatch);
                            txTemplate.execute(status -> {
                                followInfoMapper.insertBatchStg(toWrite);
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
                externalContactMapper.insertBatchStg(toWrite);
                return null;
            });
        }
        if (!followInfoWriteBatch.isEmpty()) {
            final List<VxCustomerlistdetailsFollowInfo> toWrite = new ArrayList<>(followInfoWriteBatch);
            txTemplate.execute(status -> {
                followInfoMapper.insertBatchStg(toWrite);
                return null;
            });
        }
    }
}
