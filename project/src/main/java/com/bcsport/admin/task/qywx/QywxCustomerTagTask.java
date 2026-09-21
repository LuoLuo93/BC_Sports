package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.entity.qywx.VxCorpTag;
import com.bcsport.admin.entity.qywx.VxCustomerTag;
import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsFollowInfo;
import com.bcsport.admin.entity.qywx.VxTagBatch;
import com.bcsport.admin.qywxmapper.VxCorpTagMapper;
import com.bcsport.admin.qywxmapper.VxCustomerTagMapper;
import com.bcsport.admin.qywxmapper.VxCustomerlistdetailsFollowInfoMapper;
import com.bcsport.admin.qywxmapper.VxTagBatchMapper;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    /** 异步线程等待全部打标任务完成的时限；超时后由最后一个完成的任务负责收尾落库 */
    private static final long LATCH_TIMEOUT_MINUTES = 120;
    private static final int ERRMSG_MAX = 500;
    /** 预检结果里未匹配明细的返回上限 */
    private static final int PREVIEW_DETAIL_LIMIT = 200;
    /** 未匹配标签自动创建时归属的标签组 */
    private static final String AUTO_CREATE_GROUP_NAME = "Excel导入";

    public static final String ACTION_ADD = "ADD";
    public static final String ACTION_REMOVE = "REMOVE";

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 0;
    public static final int STATUS_TAG_UNMATCHED = 2;
    public static final int STATUS_CUSTOMER_UNMATCHED = 3;

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
    private VxTagBatchMapper tagBatchMapper;

    @Autowired
    private VxCustomerlistdetailsFollowInfoMapper followInfoMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    /**
     * 同步企业标签库（定时任务方法）。
     * 批量打标进行中跳过：打标开始时对标签库做了名称→ID快照，中途变动会导致本批剩余行匹配失效。
     */
    public void syncTags() {
        synchronized (QywxCustomerTagTask.class) {
            if (batchTagging) {
                log.warn("批量打标进行中，跳过本次标签库同步");
                return;
            }
            if (syncing) {
                log.warn("同步企业标签库正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: 同步企业标签库 ===");
        try {
            doSyncTags();
        } catch (Exception e) {
            log.error("=== 失败: 同步企业标签库: {} ===", e.getMessage(), e);
            throw e;
        } finally {
            synchronized (QywxCustomerTagTask.class) {
                syncing = false;
            }
        }
    }

    private void doSyncTags() {
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
    }

    // ==================== 打标计划 ====================

    /** 单个(员工,客户)上的打标动作：新增标签集合与移除标签集合 */
    private static class PlanEntry {
        final Set<String> addTagIds = new LinkedHashSet<>();
        final Set<String> removeTagIds = new LinkedHashSet<>();
    }

    /** 校验+计划结果 */
    private static class TagPlan {
        Map<String, String> tagIdToName = new HashMap<>();
        /** userid -> externalUserid -> 动作集合 */
        Map<String, Map<String, PlanEntry>> plan = new LinkedHashMap<>();
        Set<String> unmatchedAddTagNames = new LinkedHashSet<>();
        Set<String> unmatchedRemoveTagNames = new LinkedHashSet<>();
        Set<String> unmatchedCustomerIds = new LinkedHashSet<>();
        int unmatchedTagRows;
        int unmatchedCustomerRows;
        /** 未匹配行也落流水，页面可见 */
        List<VxCustomerTag> unmatchedRecords = new ArrayList<>();

        int totalApplications() {
            int n = 0;
            for (Map<String, PlanEntry> m : plan.values()) {
                for (PlanEntry e : m.values()) {
                    n += e.addTagIds.size() + e.removeTagIds.size();
                }
            }
            return n;
        }
    }

    /**
     * 校验并构建打标计划：标签名→ID 匹配、客户→跟进人解析。
     * 预检与正式执行共用，保证两边口径一致。
     */
    private TagPlan buildPlan(List<Map<String, String>> items) {
        TagPlan p = new TagPlan();

        List<VxCorpTag> corpTags = corpTagMapper.selectAllActive();
        Map<String, String> tagNameToId = corpTags.stream()
                .collect(Collectors.toMap(VxCorpTag::getTagName, VxCorpTag::getTagId, (a, b) -> a));
        p.tagIdToName = corpTags.stream()
                .collect(Collectors.toMap(VxCorpTag::getTagId, VxCorpTag::getTagName, (a, b) -> a));

        // 提取去重的 externalUserid，按需查询 follow_info（避免全表加载）
        Set<String> distinctExtIds = items.stream()
                .map(item -> item.get("externalUserid"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Set<String>> extToUsers = new HashMap<>();
        List<String> extIdList = new ArrayList<>(distinctExtIds);
        for (int i = 0; i < extIdList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, extIdList.size());
            List<VxCustomerlistdetailsFollowInfo> followInfos = followInfoMapper.selectByExternalUserids(extIdList.subList(i, end));
            for (VxCustomerlistdetailsFollowInfo fi : followInfos) {
                extToUsers.computeIfAbsent(fi.getExternalUserid(), k -> new LinkedHashSet<>()).add(fi.getUserid());
            }
        }

        for (Map<String, String> item : items) {
            String externalUserid = item.get("externalUserid");
            String tagName = item.get("tagName");
            String action = item.getOrDefault("action", ACTION_ADD);
            boolean remove = ACTION_REMOVE.equals(action);

            String tagId = tagNameToId.get(tagName);
            if (tagId == null) {
                if (remove) {
                    p.unmatchedRemoveTagNames.add(tagName);
                } else {
                    p.unmatchedAddTagNames.add(tagName);
                }
                p.unmatchedTagRows++;
                p.unmatchedRecords.add(buildRecord(externalUserid, null, null, tagName, action,
                        STATUS_TAG_UNMATCHED, "标签库中不存在该标签", null));
                continue;
            }

            Set<String> userids = extToUsers.get(externalUserid);
            if (userids == null || userids.isEmpty()) {
                p.unmatchedCustomerIds.add(externalUserid);
                p.unmatchedCustomerRows++;
                p.unmatchedRecords.add(buildRecord(externalUserid, null, tagId, tagName, action,
                        STATUS_CUSTOMER_UNMATCHED, "客户不存在或无跟进员工", null));
                continue;
            }

            for (String userid : userids) {
                PlanEntry entry = p.plan
                        .computeIfAbsent(userid, k -> new LinkedHashMap<>())
                        .computeIfAbsent(externalUserid, k -> new PlanEntry());
                if (remove) {
                    entry.removeTagIds.add(tagId);
                } else {
                    entry.addTagIds.add(tagId);
                }
            }
        }
        return p;
    }

    /**
     * 预检（dry-run）：只校验不执行，返回匹配/未匹配统计，供上传前确认
     */
    public Map<String, Object> previewTag(List<Map<String, String>> items) {
        TagPlan plan = buildPlan(items);

        int addRows = 0;
        for (Map<String, String> item : items) {
            if (!ACTION_REMOVE.equals(item.getOrDefault("action", ACTION_ADD))) addRows++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", items.size());
        result.put("addRows", addRows);
        result.put("removeRows", items.size() - addRows);
        result.put("matchedRows", items.size() - plan.unmatchedTagRows - plan.unmatchedCustomerRows);
        result.put("unmatchedTagRows", plan.unmatchedTagRows);
        result.put("unmatchedCustomerRows", plan.unmatchedCustomerRows);
        result.put("plannedApplications", plan.totalApplications());
        result.put("unmatchedTags", limitList(new ArrayList<>(plan.unmatchedAddTagNames)));
        result.put("unmatchedRemoveTags", limitList(new ArrayList<>(plan.unmatchedRemoveTagNames)));
        result.put("unmatchedCustomers", limitList(new ArrayList<>(plan.unmatchedCustomerIds)));
        return result;
    }

    private List<String> limitList(List<String> list) {
        return list.size() > PREVIEW_DETAIL_LIMIT ? list.subList(0, PREVIEW_DETAIL_LIMIT) : list;
    }

    // ==================== 批量打标 ====================

    /** 批次上下文：计数/缓冲跨线程共享，收尾幂等。plan 在提交任务前赋值，任务线程可见 */
    private static class BatchContext {
        final String batchNo;
        final Long batchId;
        final int totalRows;
        final AtomicInteger successCount = new AtomicInteger();
        final AtomicInteger failCount = new AtomicInteger();
        final List<VxCustomerTag> recordBuffer = Collections.synchronizedList(new ArrayList<>());
        final AtomicBoolean finalized = new AtomicBoolean(false);
        volatile TagPlan plan;

        BatchContext(String batchNo, Long batchId, int totalRows) {
            this.batchNo = batchNo;
            this.batchId = batchId;
            this.totalRows = totalRows;
        }
    }

    /**
     * Excel批量打标
     * @param items Excel解析结果: externalUserid + tagName + action(ADD/REMOVE)
     * @param fileName 上传文件名（记入批次汇总）
     * @param autoCreateTags 标签库中不存在的ADD标签是否自动创建到「Excel导入」标签组
     * @return 打标结果统计（同步落库到 VX_TagBatch，返回值仅供日志）
     */
    public Map<String, Object> batchTag(List<Map<String, String>> items, String fileName, boolean autoCreateTags) {
        String batchNo = "BATCH_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        log.info("=== 开始批量打标, 批次号: {}, 共{}条 ===", batchNo, items.size());
        synchronized (QywxCustomerTagTask.class) {
            if (batchTagging) {
                log.warn("批量打标正在进行中，请勿重复操作");
                return Collections.singletonMap("error", "批量打标正在进行中，请勿重复操作");
            }
            batchTagging = true;
        }

        BatchContext ctx = null;
        TagPlan plan = null;
        try {
            // 1. 批次汇总行先落库(RUNNING)，页面可见进行中状态
            VxTagBatch batchRow = new VxTagBatch();
            batchRow.setId(IdWorker.getId());
            batchRow.setBatchNo(batchNo);
            batchRow.setFileName(fileName);
            batchRow.setStatus("RUNNING");
            batchRow.setTotalRows(items.size());
            batchRow.setStartTime(LocalDateTime.now());
            batchRow.setCreateTime(LocalDateTime.now());
            tagBatchMapper.insert(batchRow);
            ctx = new BatchContext(batchNo, batchRow.getId(), items.size());

            // 2. 校验并构建计划
            plan = buildPlan(items);

            // 3. 自动创建缺失标签后重新校验（新标签此时可匹配）
            if (autoCreateTags && !plan.unmatchedAddTagNames.isEmpty()) {
                createMissingTags(new ArrayList<>(plan.unmatchedAddTagNames));
                plan = buildPlan(items);
            }

            // 未匹配行也进流水缓冲（buildPlan 不知道批次号，此处补上）
            for (VxCustomerTag r : plan.unmatchedRecords) {
                r.setBatchNo(batchNo);
                ctx.recordBuffer.add(r);
            }

            // 4. 并行执行打标 API，边打标边落库
            ctx.plan = plan;
            int totalTasks = 0;
            for (Map.Entry<String, Map<String, PlanEntry>> userEntry : plan.plan.entrySet()) {
                for (PlanEntry entry : userEntry.getValue().values()) {
                    totalTasks += (entry.addTagIds.size() + MARK_TAG_BATCH - 1) / MARK_TAG_BATCH;
                    totalTasks += (entry.removeTagIds.size() + MARK_TAG_BATCH - 1) / MARK_TAG_BATCH;
                }
            }
            Semaphore semaphore = new Semaphore(API_CONCURRENCY);
            CountDownLatch latch = new CountDownLatch(totalTasks);
            Map<String, String> tagIdToName = plan.tagIdToName;

            for (Map.Entry<String, Map<String, PlanEntry>> userEntry : plan.plan.entrySet()) {
                String userid = userEntry.getKey();

                for (Map.Entry<String, PlanEntry> ctEntry : userEntry.getValue().entrySet()) {
                    String externalUserid = ctEntry.getKey();
                    PlanEntry entry = ctEntry.getValue();

                    submitChunks(latch, semaphore, ctx, tagIdToName, userid, externalUserid,
                            ACTION_ADD, new ArrayList<>(entry.addTagIds));
                    submitChunks(latch, semaphore, ctx, tagIdToName, userid, externalUserid,
                            ACTION_REMOVE, new ArrayList<>(entry.removeTagIds));
                }
            }

            // 5. 等待全部完成；最后一个完成的任务也会尝试收尾（幂等），此处兜底
            boolean completed = false;
            try {
                completed = latch.await(LATCH_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (latch.getCount() == 0) {
                finalizeBatch(ctx);
            } else if (!completed) {
                log.error("=== 批量打标等待超时({}分钟), 批次号: {}, 剩余{}个任务未完成, 本地记录可能不全 ===",
                        LATCH_TIMEOUT_MINUTES, batchNo, latch.getCount());
                flushRecords(ctx.recordBuffer);
                updateBatchRow(ctx.batchId, b -> {
                    b.setStatus("FAILED");
                    b.setErrmsg("等待超时(" + LATCH_TIMEOUT_MINUTES + "分钟)，部分打标任务未完成，企微侧结果以实际为准");
                    b.setEndTime(LocalDateTime.now());
                });
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("batchNo", batchNo);
            summary.put("total", items.size());
            summary.put("success", ctx.successCount.get());
            summary.put("fail", ctx.failCount.get());
            summary.put("unmatchedTags", plan.unmatchedAddTagNames.size() + plan.unmatchedRemoveTagNames.size());
            summary.put("unmatchedCustomers", plan.unmatchedCustomerIds.size());
            log.info("=== 完成: 批量打标, 批次号: {}, 成功: {}, 失败: {}, 未匹配标签: {}, 未匹配客户: {} ===",
                    batchNo, ctx.successCount.get(), ctx.failCount.get(),
                    plan.unmatchedAddTagNames.size() + plan.unmatchedRemoveTagNames.size(), plan.unmatchedCustomerIds.size());
            return summary;
        } catch (Exception e) {
            log.error("=== 失败: 批量打标, 批次号: {} ===", batchNo, e);
            if (ctx != null) {
                flushRecords(ctx.recordBuffer);
                Long batchId = ctx.batchId;
                String msg = truncate(e.getMessage(), ERRMSG_MAX);
                updateBatchRow(batchId, b -> {
                    b.setStatus("FAILED");
                    b.setErrmsg(msg);
                    b.setEndTime(LocalDateTime.now());
                });
            }
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            synchronized (QywxCustomerTagTask.class) {
                batchTagging = false;
            }
        }
    }

    private void submitChunks(CountDownLatch latch, Semaphore semaphore, BatchContext ctx,
                              Map<String, String> tagIdToName, String userid, String externalUserid,
                              String action, List<String> tagIds) {
        for (int i = 0; i < tagIds.size(); i += MARK_TAG_BATCH) {
            List<String> batch = tagIds.subList(i, Math.min(i + MARK_TAG_BATCH, tagIds.size()));
            final String act = action;
            final List<String> chunk = new ArrayList<>(batch);
            taskThreadPool.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        JSONObject result = act.equals(ACTION_ADD)
                                ? apiClient.markTag(userid, externalUserid, chunk, null)
                                : apiClient.markTag(userid, externalUserid, null, chunk);
                        Integer errcode = result.getInt("errcode");
                        if (errcode != null && errcode == 0) {
                            ctx.successCount.addAndGet(chunk.size());
                            for (String tid : chunk) {
                                ctx.recordBuffer.add(buildRecord(externalUserid, userid, tid,
                                        tagIdToName.getOrDefault(tid, ""), act, STATUS_SUCCESS, null, ctx.batchNo));
                            }
                            if (ctx.recordBuffer.size() >= FLUSH_THRESHOLD) {
                                flushRecords(ctx.recordBuffer);
                            }
                        } else {
                            ctx.failCount.addAndGet(chunk.size());
                            String errmsg = "errcode: " + errcode + ", errmsg: " + result.getStr("errmsg");
                            log.warn("打标失败, userid: {}, externalUserid: {}, errcode: {}, errmsg: {}",
                                    userid, externalUserid, errcode, result.getStr("errmsg"));
                            for (String tid : chunk) {
                                ctx.recordBuffer.add(buildRecord(externalUserid, userid, tid,
                                        tagIdToName.getOrDefault(tid, ""), act, STATUS_FAIL,
                                        truncate(errmsg, ERRMSG_MAX), ctx.batchNo));
                            }
                        }
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ctx.failCount.addAndGet(chunk.size());
                } catch (Exception e) {
                    ctx.failCount.addAndGet(chunk.size());
                    log.error("打标异常, userid: {}, externalUserid: {}: {}", userid, externalUserid, e.getMessage());
                    for (String tid : chunk) {
                        ctx.recordBuffer.add(buildRecord(externalUserid, userid, tid,
                                tagIdToName.getOrDefault(tid, ""), act, STATUS_FAIL,
                                truncate(e.getMessage(), ERRMSG_MAX), ctx.batchNo));
                    }
                } finally {
                    latch.countDown();
                    // 最后一个完成的任务负责收尾，避免主线程超时返回后丢失尾巴记录
                    if (latch.getCount() == 0) {
                        try {
                            finalizeBatch(ctx);
                        } catch (Exception fe) {
                            log.error("批次收尾失败, batchNo: {}", ctx.batchNo, fe);
                        }
                    }
                }
            });
        }
    }

    /**
     * 批次收尾：剩余流水落库 + 批次汇总置 DONE。幂等（最后一个任务与主线程都可能触发）。
     */
    private void finalizeBatch(BatchContext ctx) {
        if (!ctx.finalized.compareAndSet(false, true)) {
            return;
        }
        flushRecords(ctx.recordBuffer);
        TagPlan plan = ctx.plan;
        final int success = ctx.successCount.get();
        final int fail = ctx.failCount.get();
        final int unmatchedTagRows = plan != null ? plan.unmatchedTagRows : 0;
        final int unmatchedCustomerRows = plan != null ? plan.unmatchedCustomerRows : 0;
        updateBatchRow(ctx.batchId, b -> {
            b.setStatus("DONE");
            b.setSuccessCnt(success);
            b.setFailCnt(fail);
            b.setUnmatchedTagRows(unmatchedTagRows);
            b.setUnmatchedCustomerRows(unmatchedCustomerRows);
            b.setEndTime(LocalDateTime.now());
        });
    }

    private void updateBatchRow(Long batchId, java.util.function.Consumer<VxTagBatch> updater) {
        if (batchId == null) return;
        try {
            VxTagBatch row = new VxTagBatch();
            row.setId(batchId);
            updater.accept(row);
            tagBatchMapper.updateById(row);
        } catch (Exception e) {
            log.error("更新批次汇总失败, batchId: {}", batchId, e);
        }
    }

    /**
     * 把标签库中不存在的标签创建到「Excel导入」标签组（组不存在则一并创建），
     * 并写入本地标签库表，调用方随后重建计划即可匹配。
     */
    private void createMissingTags(List<String> tagNames) {
        // 名称去重保序
        List<String> toCreate = new ArrayList<>(new LinkedHashSet<>(tagNames));
        if (toCreate.isEmpty()) return;

        List<VxCorpTag> corpTags = corpTagMapper.selectAllActive();
        String groupId = null;
        for (VxCorpTag t : corpTags) {
            if (t.getGroupId() == null && AUTO_CREATE_GROUP_NAME.equals(t.getTagName())) {
                groupId = t.getTagId();
                break;
            }
        }

        JSONObject resp = apiClient.addCorpTag(groupId, groupId == null ? AUTO_CREATE_GROUP_NAME : null, toCreate);
        JSONObject group = resp.getJSONObject("tag_group");
        if (group == null) {
            throw new RuntimeException("自动创建标签失败: 响应缺少tag_group");
        }
        String newGroupId = group.getStr("group_id");
        String newGroupName = group.getStr("group_name", AUTO_CREATE_GROUP_NAME);

        List<VxCorpTag> newRows = new ArrayList<>();
        if (groupId == null) {
            VxCorpTag groupRow = new VxCorpTag();
            groupRow.setId(IdWorker.getId());
            groupRow.setTagId(newGroupId);
            groupRow.setTagName(newGroupName);
            groupRow.setGroupId(null);
            groupRow.setGroupName(newGroupName);
            groupRow.setSortOrder(0);
            groupRow.setDeleted(0);
            newRows.add(groupRow);
        }
        JSONArray tags = group.getJSONArray("tag");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                VxCorpTag row = new VxCorpTag();
                row.setId(IdWorker.getId());
                row.setTagId(tag.getStr("id"));
                row.setTagName(tag.getStr("name"));
                row.setGroupId(newGroupId);
                row.setGroupName(newGroupName);
                row.setSortOrder(tag.getInt("order", 0));
                row.setDeleted(0);
                newRows.add(row);
            }
        }
        corpTagMapper.insertBatch(newRows);
        log.info("自动创建缺失标签{}个, 标签组: {}", newRows.size() - (groupId == null ? 1 : 0), AUTO_CREATE_GROUP_NAME);
    }

    private VxCustomerTag buildRecord(String externalUserid, String userid, String tagId, String tagName,
                                      String action, int status, String errmsg, String batchNo) {
        VxCustomerTag record = new VxCustomerTag();
        record.setId(IdWorker.getId());
        record.setExternalUserid(externalUserid);
        record.setUserid(userid);
        record.setTagId(tagId);
        record.setTagName(tagName);
        record.setSource("IMPORT");
        record.setBatchNo(batchNo);
        record.setStatus(status);
        record.setErrmsg(errmsg);
        record.setTagAction(action);
        return record;
    }

    /**
     * 异步批量打标（fire-and-forget，结果落在 VX_TagBatch / VX_CustomerTag）
     */
    public void batchTagAsync(List<Map<String, String>> items, String fileName, boolean autoCreateTags) {
        try {
            batchTag(items, fileName, autoCreateTags);
        } catch (Exception e) {
            log.error("异步批量打标异常: {}", e.getMessage(), e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
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
