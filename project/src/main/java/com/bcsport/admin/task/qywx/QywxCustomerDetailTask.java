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
    /** 游标翻页熔断兜底上限：真回环由 next_cursor 重复检测精确识别，此上限只防"页页有数据"的异常超量翻页 */
    private static final int MAX_CURSOR_PAGES = 2000;
    /** 连续空页判定阈值：连续多页无数据但 next_cursor 仍不终止，判定服务端游标异常且数据已到尾，本批按完整结束 */
    private static final int MAX_CONSECUTIVE_EMPTY_PAGES = 10;
    /** 切换时分批回填每批行数：单条 INSERT...SELECT 秒级完成，避免整表回填长时间占住 socket 读触发 Read timed out */
    private static final int COPY_CHUNK_SIZE = 50_000;

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
            int truncatedBatches = result[2];
            int totalBatches = (validUserList.size() + USER_BATCH_SIZE - 1) / USER_BATCH_SIZE;

            // 4a. 任一批次游标翻页熔断 → 影子表确定不完整，直接放弃切换（旧数据只旧一天，残缺数据无法补救）
            if (truncatedBatches > 0) {
                throw new IllegalStateException(String.format(
                        "%d个批次触发游标翻页熔断，影子表数据不完整，保留主表旧数据不切换", truncatedBatches));
            }

            // 4b. 失败率超10%(典型如token失效导致全部批次失败)时放弃切换，保留主表旧数据
            if (failedBatches * 10 > totalBatches) {
                throw new IllegalStateException(String.format(
                        "同步客户详情失败率过高(失败批次 %d/%d)，保留主表旧数据不切换", failedBatches, totalBatches));
            }

            // 5. 原子切换：主表清空+影子表分批回填+清影子表，同一事务，任一步失败整体回滚
            new TransactionTemplate(transactionManager).execute(status -> {
                externalContactMapper.deleteAll();
                copyStgInChunks("external_contact", externalContactMapper::copyFromStgPage);
                externalContactMapper.clearStg();
                followInfoMapper.deleteAll();
                copyStgInChunks("follow_info", followInfoMapper::copyFromStgPage);
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
     * @return [成功批次数, 失败批次数, 游标回环熔断批次数]
     */
    private int[] syncCustomerDetails(List<String> followUserList) {
        long startTime = System.currentTimeMillis();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger truncatedCount = new AtomicInteger(0);
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
                } catch (CursorLoopException e) {
                    // 游标翻页熔断：真回环(重复页会膨胀跟进信息)或超量翻页，两种情况本批数据都不完整
                    log.error("处理批次 {}/{} 触发游标翻页熔断: {}", currentBatch + 1, totalBatches, e.getMessage());
                    truncatedCount.incrementAndGet();
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
            boolean completed = totalLatch.await(45, TimeUnit.MINUTES);
            if (!completed) {
                // 超时=部分批次未执行完，影子表数据不完整，必须放弃切换保住主表旧数据
                // 45分钟=兜底页上限2000页按0.7s/页约23分钟+写库耗时的余量
                throw new IllegalStateException("等待客户详情批次超时(45分钟)，本轮放弃切换");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("客户详情同步被中断，本轮放弃切换", e);
        }

        log.info("客户详情同步完成, 成功: {}, 失败: {}, 游标回环熔断: {}, contacts: {}, followInfos: {}, 耗时: {} ms",
                successCount.get(), failCount.get(), truncatedCount.get(), totalContacts.get(), totalFollowInfos.get(),
                System.currentTimeMillis() - startTime);
        return new int[]{successCount.get(), failCount.get(), truncatedCount.get()};
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
        int cursorPages = 0;
        // 游标异常诊断三件套：见过的cursor(真回环精确检测)、连续空页数(游标不终止但数据到尾)、本批累计行数(超量翻页判据)
        Set<String> seenCursors = new HashSet<>();
        int consecutiveEmptyPages = 0;
        int contactsInBatch = 0;
        int followInfosInBatch = 0;
        do {
            if (++cursorPages > MAX_CURSOR_PAGES) {
                // 抛专用异常而不是静默中止：熔断意味着本批数据不完整，上层必须据此放弃影子表切换
                throw new CursorLoopException(String.format(
                        "游标翻页超过%d页上限, 连续空页: %d, 本批已拉取 contacts: %d, followInfos: %d"
                                + "(若连续空页为0且contacts接近页数上限x100，说明该批真实客户量超上限，应减小USER_BATCH_SIZE)",
                        MAX_CURSOR_PAGES, consecutiveEmptyPages, contactsInBatch, followInfosInBatch));
            }
            // 1. 请求一页
            JSONObject result = apiClient.batchGetByUser(userIds, cursor);
            if (result == null) break;

            // 2. 解析这一页
            JSONArray externalContactList = result.getJSONArray("external_contact_list");
            int itemsThisPage = externalContactList == null ? 0 : externalContactList.size();
            consecutiveEmptyPages = (itemsThisPage == 0) ? consecutiveEmptyPages + 1 : 0;
            if (itemsThisPage > 0) {
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
                        contactsInBatch++;

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
                        followInfosInBatch++;

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

            // 3. 取下一页cursor：空=正常完结；非空则查重(真回环)，并判定"连续空页+游标不终止"型异常
            String nextCursor = result.getStr("next_cursor", "");
            if (nextCursor == null || nextCursor.isEmpty()) {
                break;
            }
            if (!seenCursors.add(nextCursor)) {
                throw new CursorLoopException(String.format(
                        "next_cursor重复出现(真实回环), 已拉取%d页, 本批 contacts: %d, followInfos: %d",
                        cursorPages, contactsInBatch, followInfosInBatch));
            }
            if (consecutiveEmptyPages >= MAX_CONSECUTIVE_EMPTY_PAGES) {
                // 数据若未到尾页内应有数据；连续空页说明已到尾，服务端只是游标不置空，本批按完整成功结束
                log.warn("连续{}页空数据但next_cursor未终止(服务端游标异常)，判定本批数据已拉取完整: 共{}页, contacts: {}, followInfos: {}",
                        consecutiveEmptyPages, cursorPages, contactsInBatch, followInfosInBatch);
                break;
            }
            cursor = nextCursor;
        } while (true);

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

    /** 游标回环熔断专用异常：触发即代表该批数据不完整，本轮必须放弃影子表切换 */
    private static class CursorLoopException extends RuntimeException {
        CursorLoopException(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    private interface StgPageCopier {
        int copyPage(long offset, int limit);
    }

    /**
     * 影子表分批回填主表：循环按 OFFSET/FETCH 翻页回填，返回值小于批行数即读完
     * （整表一条 INSERT...SELECT 约50万行时，socket 读等待可能先于语句完成而超时，且中途失败整连接报废）
     */
    private long copyStgInChunks(String tableLabel, StgPageCopier copier) {
        long total = 0;
        for (long offset = 0; ; offset += COPY_CHUNK_SIZE) {
            int rows = copier.copyPage(offset, COPY_CHUNK_SIZE);
            total += rows;
            if (rows < COPY_CHUNK_SIZE) {
                break;
            }
        }
        log.info("影子表[{}]分批回填完成, 共 {} 行, 每批 {} 行", tableLabel, total, COPY_CHUNK_SIZE);
        return total;
    }
}
