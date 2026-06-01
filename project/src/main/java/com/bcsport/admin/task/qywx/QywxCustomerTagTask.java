package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.entity.qywx.VxCorpTag;
import com.bcsport.admin.entity.qywx.VxCustomerTag;
import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsFollowInfo;
import com.bcsport.admin.qywxmapper.VxCorpTagMapper;
import com.bcsport.admin.qywxmapper.VxCustomerTagMapper;
import com.bcsport.admin.qywxmapper.VxCustomerlistdetailsFollowInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 企微客户标签任务
 */
@Slf4j
@Component("qywxCustomerTagTask")
public class QywxCustomerTagTask {

    private static volatile boolean syncing = false;
    private static volatile boolean batchTagging = false;

    public static boolean isSyncing() { return syncing; }
    public static boolean isBatchTagging() { return batchTagging; }

    private static final int BATCH_SIZE = 100;
    private static final int MARK_TAG_BATCH = 50;
    private static final int FLUSH_THRESHOLD = 500;
    private static final int API_CONCURRENCY = 3;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @Autowired
    private VxCorpTagMapper corpTagMapper;

    @Autowired
    private VxCustomerTagMapper customerTagMapper;

    @Autowired
    private VxCustomerlistdetailsFollowInfoMapper followInfoMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    /**
     * 同步企业标签库（定时任务方法）
     */
    public void syncTags() {
        synchronized (QywxCustomerTagTask.class) {
            if (syncing) {
                log.warn("同步企业标签库正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: 同步企业标签库 ===");
        try {
            // --- HTTP call: outside transaction ---
            JSONObject result = apiClient.getCorpTagList(null, null);
            JSONArray tagGroupArray = result.getJSONArray("tag_group");
            if (tagGroupArray == null || tagGroupArray.isEmpty()) {
                log.warn("=== 完成: 企微标签库为空 ===");
                return;
            }

            // --- Build data in memory: outside transaction ---
            List<VxCorpTag> allTags = new ArrayList<>();
            for (int i = 0; i < tagGroupArray.size(); i++) {
                JSONObject group = tagGroupArray.getJSONObject(i);
                String groupId = group.getStr("group_id");
                String groupName = group.getStr("group_name");

                // 插入标签组本身作为父级记录
                VxCorpTag groupTag = new VxCorpTag();
                groupTag.setId(IdWorker.getId());
                groupTag.setTagId(groupId);
                groupTag.setTagName(groupName);
                groupTag.setGroupId(null);
                groupTag.setGroupName(groupName);
                groupTag.setSortOrder(group.getInt("order", 0));
                groupTag.setDeleted(0);
                allTags.add(groupTag);

                // 插入标签组下的子标签
                JSONArray tags = group.getJSONArray("tag");
                if (tags != null) {
                    for (int j = 0; j < tags.size(); j++) {
                        JSONObject tag = tags.getJSONObject(j);
                        VxCorpTag corpTag = new VxCorpTag();
                        corpTag.setId(IdWorker.getId());
                        corpTag.setTagId(tag.getStr("id"));
                        corpTag.setTagName(tag.getStr("name"));
                        corpTag.setGroupId(groupId);
                        corpTag.setGroupName(groupName);
                        corpTag.setSortOrder(tag.getInt("order", 0));
                        corpTag.setDeleted(0);
                        allTags.add(corpTag);
                    }
                }
            }

            // --- DB write: short transaction ---
            final List<VxCorpTag> tagsToInsert = allTags;
            new TransactionTemplate(transactionManager).execute(status -> {
                corpTagMapper.deleteAll();
                for (int i = 0; i < tagsToInsert.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, tagsToInsert.size());
                    corpTagMapper.insertBatch(tagsToInsert.subList(i, end));
                }
                return null;
            });

            log.info("=== 完成: 同步企业标签库, 共{}个标签 ===", allTags.size());
        } catch (Exception e) {
            log.error("=== 失败: 同步企业标签库: {} ===", e.getMessage(), e);
            throw e;
        } finally {
            synchronized (QywxCustomerTagTask.class) {
                syncing = false;
            }
        }
    }

    /**
     * Excel批量打标
     * @param items Excel解析结果: externalUserid + tagName
     * @return 打标结果统计
     */
    public Map<String, Object> batchTag(List<Map<String, String>> items) {
        String batchNo = "BATCH_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        log.info("=== 开始批量打标, 批次号: {}, 共{}条 ===", batchNo, items.size());
        synchronized (QywxCustomerTagTask.class) {
            if (batchTagging) {
                log.warn("批量打标正在进行中，请勿重复操作");
                return Collections.singletonMap("error", "批量打标正在进行中，请勿重复操作");
            }
            batchTagging = true;
        }

        try {
        // 1. 加载标签库 Map<tagName, tagId>
        List<VxCorpTag> corpTags = corpTagMapper.selectAllActive();
        Map<String, String> tagNameToId = corpTags.stream()
                .collect(Collectors.toMap(VxCorpTag::getTagName, VxCorpTag::getTagId, (a, b) -> a));
        Map<String, String> tagIdToName = corpTags.stream()
                .collect(Collectors.toMap(VxCorpTag::getTagId, VxCorpTag::getTagName, (a, b) -> a));

        // 2. 提取去重的 externalUserid，按需查询 follow_info（避免全表加载）
        Set<String> distinctExtIds = items.stream()
                .map(item -> item.get("externalUserid"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Set<String>> extToUsers = new HashMap<>();
        List<String> extIdList = new ArrayList<>(distinctExtIds);
        for (int i = 0; i < extIdList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, extIdList.size());
            List<String> subList = extIdList.subList(i, end);
            List<VxCustomerlistdetailsFollowInfo> followInfos = followInfoMapper.selectByExternalUserids(subList);
            for (VxCustomerlistdetailsFollowInfo fi : followInfos) {
                extToUsers.computeIfAbsent(fi.getExternalUserid(), k -> new LinkedHashSet<>()).add(fi.getUserid());
            }
        }

        // 3. 校验并收集打标计划
        Set<String> unmatchedTags = new LinkedHashSet<>();
        Set<String> unmatchedCustomers = new LinkedHashSet<>();
        Map<String, Map<String, Set<String>>> plan = new LinkedHashMap<>();

        for (Map<String, String> item : items) {
            String externalUserid = item.get("externalUserid");
            String tagName = item.get("tagName");

            String tagId = tagNameToId.get(tagName);
            if (tagId == null) {
                unmatchedTags.add(tagName);
                continue;
            }

            Set<String> userids = extToUsers.get(externalUserid);
            if (userids == null || userids.isEmpty()) {
                unmatchedCustomers.add(externalUserid);
                continue;
            }

            for (String userid : userids) {
                plan.computeIfAbsent(userid, k -> new LinkedHashMap<>())
                        .computeIfAbsent(externalUserid, k -> new LinkedHashSet<>())
                        .add(tagId);
            }
        }

        // 4. 并行执行打标 API，边打标边落库（使用统一线程池）
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<VxCustomerTag> recordBuffer = Collections.synchronizedList(new ArrayList<>());

        int totalTasks = 0;
        for (Map.Entry<String, Map<String, Set<String>>> userEntry : plan.entrySet()) {
            for (Map.Entry<String, Set<String>> ctEntry : userEntry.getValue().entrySet()) {
                totalTasks += (ctEntry.getValue().size() + MARK_TAG_BATCH - 1) / MARK_TAG_BATCH;
            }
        }
        Semaphore semaphore = new Semaphore(API_CONCURRENCY);
        CountDownLatch latch = new CountDownLatch(totalTasks);

        for (Map.Entry<String, Map<String, Set<String>>> userEntry : plan.entrySet()) {
            String userid = userEntry.getKey();
            Map<String, Set<String>> customerTags = userEntry.getValue();

            for (Map.Entry<String, Set<String>> ctEntry : customerTags.entrySet()) {
                String externalUserid = ctEntry.getKey();
                List<String> addTagList = new ArrayList<>(ctEntry.getValue());

                for (int i = 0; i < addTagList.size(); i += MARK_TAG_BATCH) {
                    List<String> batch = addTagList.subList(i, Math.min(i + MARK_TAG_BATCH, addTagList.size()));
                    taskThreadPool.submit(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                JSONObject result = apiClient.markTag(userid, externalUserid, batch, null);
                                Integer errcode = result.getInt("errcode");
                                if (errcode != null && errcode == 0) {
                                    successCount.addAndGet(batch.size());
                                    for (String tid : batch) {
                                        VxCustomerTag record = new VxCustomerTag();
                                        record.setId(IdWorker.getId());
                                        record.setExternalUserid(externalUserid);
                                        record.setUserid(userid);
                                        record.setTagId(tid);
                                        record.setTagName(tagIdToName.getOrDefault(tid, ""));
                                        record.setSource("IMPORT");
                                        record.setBatchNo(batchNo);
                                        recordBuffer.add(record);
                                    }
                                    if (recordBuffer.size() >= FLUSH_THRESHOLD) {
                                        flushRecords(recordBuffer);
                                    }
                                } else {
                                    failCount.addAndGet(batch.size());
                                    log.warn("打标失败, userid: {}, externalUserid: {}, errcode: {}, errmsg: {}",
                                            userid, externalUserid, errcode, result.getStr("errmsg"));
                                }
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            failCount.addAndGet(batch.size());
                        } catch (Exception e) {
                            failCount.addAndGet(batch.size());
                            log.error("打标异常, userid: {}, externalUserid: {}: {}",
                                    userid, externalUserid, e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }
        }

        try {
            latch.await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 落库剩余记录
        flushRecords(recordBuffer);

        log.info("=== 完成: 批量打标, 成功: {}, 失败: {}, 未匹配标签: {}, 未匹配客户: {} ===",
                successCount.get(), failCount.get(), unmatchedTags.size(), unmatchedCustomers.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("batchNo", batchNo);
        summary.put("total", items.size());
        summary.put("success", successCount.get());
        summary.put("fail", failCount.get());
        summary.put("unmatchedTags", new ArrayList<>(unmatchedTags));
        summary.put("unmatchedCustomers", new ArrayList<>(unmatchedCustomers));
            return summary;
        } finally {
            synchronized (QywxCustomerTagTask.class) {
                batchTagging = false;
            }
        }
    }

    /**
     * 异步批量打标（fire-and-forget）
     */
    public void batchTagAsync(List<Map<String, String>> items) {
        try {
            batchTag(items);
        } catch (Exception e) {
            log.error("异步批量打标异常: {}", e.getMessage(), e);
        } finally {
            synchronized (QywxCustomerTagTask.class) {
                batchTagging = false;
            }
        }
    }

    private void flushRecords(List<VxCustomerTag> buffer) {
        if (buffer.isEmpty()) return;
        List<VxCustomerTag> copy;
        synchronized (buffer) {
            copy = new ArrayList<>(buffer);
            buffer.clear();
        }
        for (int i = 0; i < copy.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, copy.size());
            customerTagMapper.insertBatch(copy.subList(i, end));
        }
    }
}
