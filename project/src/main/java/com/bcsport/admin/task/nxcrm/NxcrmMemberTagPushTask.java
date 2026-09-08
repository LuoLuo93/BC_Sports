package com.bcsport.admin.task.nxcrm;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bcsport.admin.entity.ihr.NxcrmMemberTagDetail;
import com.bcsport.admin.entity.ihr.NxcrmTagInfo;
import com.bcsport.admin.entity.ihr.NxcrmTagValue;
import com.bcsport.admin.ihrmapper.NxcrmMemberTagDetailMapper;
import com.bcsport.admin.ihrmapper.NxcrmTagInfoMapper;
import com.bcsport.admin.ihrmapper.NxcrmTagValueMapper;
import com.nascent.ecrp.opensdk.domain.customer.tag.TagSetData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component("nxcrmMemberTagPushTask")
public class NxcrmMemberTagPushTask {

    private static volatile boolean syncing = false;

    public static boolean isSyncing() { return syncing; }

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int API_CONCURRENCY = 3;
    /** keyset 分页单页行数：页内行按 nasOuid 连续，跨页会员由粘合逻辑处理 */
    private static final int DETAIL_PAGE_SIZE = 5000;

    @Autowired
    private NxcrmTagInfoMapper tagInfoMapper;

    @Autowired
    private NxcrmTagValueMapper tagValueMapper;

    @Autowired
    private NxcrmMemberTagDetailMapper memberTagDetailMapper;

    @Autowired
    private NxCrmApiClient nxCrmApiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    public void pushMemberTags(Map<String, String> params) {
        synchronized (NxcrmMemberTagPushTask.class) {
            if (syncing) {
                log.warn("NXCRM推送会员标签正在进行中，请勿重复操作");
                return;
            }
            syncing = true;
        }
        log.info("=== 开始执行: NXCRM推送会员标签 ===");
        try {
            // 1. 参数校验
            int batchSize = DEFAULT_BATCH_SIZE;
            log.info("参数: batchSize={}", batchSize);

            // 2. 加载映射表
            List<NxcrmTagInfo> allTagInfos = tagInfoMapper.selectList(null);
            Map<String, NxcrmTagInfo> tagNameToInfo = allTagInfos.stream()
                .collect(Collectors.toMap(NxcrmTagInfo::getTagName, t -> t, (a, b) -> a));
            log.info("加载标签定义{}条", allTagInfos.size());

            List<NxcrmTagValue> allTagValues = tagValueMapper.selectList(null);
            Map<String, Map<String, String>> tagCodeToValueNameToCode = allTagValues.stream()
                .collect(Collectors.groupingBy(
                    NxcrmTagValue::getTagCode,
                    Collectors.toMap(NxcrmTagValue::getTagValueName, NxcrmTagValue::getTagValueCode, (a, b) -> a)
                ));
            log.info("加载标签值定义{}条", allTagValues.size());

            // 3. keyset 分页流式读取会员标签明细。此前是全表 selectList + 内存 groupingBy：
            //    行数=会员×人均标签数，随同步持续增长必 OOM；分页后内存恒定为单页大小。
            //    同一会员的明细可能被分页劈开，用"当前会员粘合"跨页累积。
            PushContext ctx = new PushContext();
            Map<String, ProfileBucket> buckets = new LinkedHashMap<>();
            AtomicInteger skipCount = new AtomicInteger(0);
            AtomicInteger memberCount = new AtomicInteger(0);

            String cursor = null;
            Map<String, TagSetData> currentTagMap = new LinkedHashMap<>();
            long totalRows = 0;
            while (true) {
                List<NxcrmMemberTagDetail> page = memberTagDetailMapper.selectList(
                        new LambdaQueryWrapper<NxcrmMemberTagDetail>()
                                .gt(cursor != null, NxcrmMemberTagDetail::getNasOuid, cursor)
                                .orderByAsc(NxcrmMemberTagDetail::getNasOuid)
                                .last("FETCH FIRST " + DETAIL_PAGE_SIZE + " ROWS ONLY"));
                if (page.isEmpty()) {
                    break;
                }
                totalRows += page.size();

                for (NxcrmMemberTagDetail detail : page) {
                    String nasOuid = detail.getNasOuid();
                    if (!nasOuid.equals(cursor)) {
                        // 新会员：结算上一个
                        if (cursor != null && !currentTagMap.isEmpty()) {
                            emitMemberToBucket(cursor, currentTagMap, buckets, batchSize, ctx);
                            memberCount.incrementAndGet();
                        }
                        cursor = nasOuid;
                        currentTagMap = new LinkedHashMap<>();
                    }
                    accumulateDetail(detail, currentTagMap, tagNameToInfo, tagCodeToValueNameToCode, skipCount);
                }

                if (page.size() < DETAIL_PAGE_SIZE) {
                    break;
                }
            }
            // 结算最后一个会员
            if (cursor != null && !currentTagMap.isEmpty()) {
                emitMemberToBucket(cursor, currentTagMap, buckets, batchSize, ctx);
                memberCount.incrementAndGet();
            }

            if (skipCount.get() > 0) {
                log.warn("跳过无法映射的标签记录{}条", skipCount.get());
            }
            log.info("流式构建完成, 明细{}行, 会员{}个, 标签组合{}种", totalRows, memberCount.get(), buckets.size());

            // 4. 冲刷各组合桶的尾批并等待全部推送完成
            for (ProfileBucket bucket : buckets.values()) {
                if (!bucket.pendingNasOuids.isEmpty()) {
                    ctx.submit(new ArrayList<>(bucket.pendingNasOuids), bucket.tagDataList);
                    bucket.pendingNasOuids.clear();
                }
            }
            buckets.clear();

            ctx.phaser.arriveAndAwaitAdvance();
            log.info("=== 完成执行: NXCRM推送会员标签, 成功={}, 失败={} ===",
                    ctx.success.get(), ctx.failed.get());
        } catch (Exception e) {
            log.error("=== 失败执行: NXCRM推送会员标签 ===", e);
            throw new RuntimeException(e);
        } finally {
            synchronized (NxcrmMemberTagPushTask.class) {
                syncing = false;
            }
        }
    }

    /** 标签组合桶：同一组合的会员凑满一批即推送并清空，尾批在收尾统一冲刷 */
    private static final class ProfileBucket {
        final List<TagSetData> tagDataList;
        final List<String> pendingNasOuids = new ArrayList<>();

        ProfileBucket(List<TagSetData> tagDataList) {
            this.tagDataList = tagDataList;
        }
    }

    /** 单次运行的推送上下文：并发原语与计数器随任务实例化，避免任务间共享 */
    private final class PushContext {
        final Semaphore semaphore = new Semaphore(API_CONCURRENCY);
        final Phaser phaser = new Phaser(1);
        final AtomicInteger success = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);

        void submit(List<String> batch, List<TagSetData> tagDataList) {
            phaser.register();
            taskThreadPool.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        nxCrmApiClient.multipleCustomerTagSet(batch, tagDataList);
                        success.addAndGet(batch.size());
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed.addAndGet(batch.size());
                } catch (Exception e) {
                    log.error("推送标签批次失败: {}", e.getMessage());
                    failed.addAndGet(batch.size());
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
    }

    /** 单行明细累积到当前会员的 tagCode→TagSetData 映射 */
    private void accumulateDetail(NxcrmMemberTagDetail detail, Map<String, TagSetData> tagMap,
                                  Map<String, NxcrmTagInfo> tagNameToInfo,
                                  Map<String, Map<String, String>> tagCodeToValueNameToCode,
                                  AtomicInteger skipCount) {
        NxcrmTagInfo tagInfo = tagNameToInfo.get(detail.getTagName());
        if (tagInfo == null) {
            log.warn("标签名未找到对应tagCode: tagName={}, nasOuid={}", detail.getTagName(), detail.getNasOuid());
            skipCount.incrementAndGet();
            return;
        }
        if (detail.getTagValueName() == null) {
            skipCount.incrementAndGet();
            return;
        }
        TagSetData tsd = tagMap.computeIfAbsent(tagInfo.getTagCode(), k -> {
            TagSetData t = new TagSetData();
            t.setTagCode(k);
            return t;
        });
        if (tsd.getTagValueList() == null) {
            tsd.setTagValueList(new ArrayList<>());
        }
        tsd.getTagValueList().add(detail.getTagValueName());
        // 预定义值标签额外赋值 tagValueCodeList
        if (tagInfo.getHasValue() != null && tagInfo.getHasValue() == 1) {
            Map<String, String> valueNameMap = tagCodeToValueNameToCode.get(tagInfo.getTagCode());
            if (valueNameMap != null && valueNameMap.containsKey(detail.getTagValueName())) {
                if (tsd.getTagValueCodeList() == null) {
                    tsd.setTagValueCodeList(new ArrayList<>());
                }
                tsd.getTagValueCodeList().add(valueNameMap.get(detail.getTagValueName()));
            }
        }
    }

    /** 一个会员的标签构建完成：按标签组合入桶，桶满即提交推送批（排序确保组合key确定性） */
    private void emitMemberToBucket(String nasOuid, Map<String, TagSetData> tagMap,
                                    Map<String, ProfileBucket> buckets, int batchSize, PushContext ctx) {
        List<TagSetData> validTags = tagMap.values().stream()
                .filter(t -> t.getTagValueList() != null && !t.getTagValueList().isEmpty())
                .sorted(Comparator.comparing(TagSetData::getTagCode))
                .collect(Collectors.toList());
        if (validTags.isEmpty()) {
            log.warn("nasOuid={}, 标签{}条全部匹配失败, 跳过推送", nasOuid, tagMap.size());
            return;
        }
        String profileKey = JSONUtil.toJsonStr(validTags);
        ProfileBucket bucket = buckets.computeIfAbsent(profileKey, k -> new ProfileBucket(validTags));
        bucket.pendingNasOuids.add(nasOuid);
        if (bucket.pendingNasOuids.size() >= batchSize) {
            ctx.submit(new ArrayList<>(bucket.pendingNasOuids), bucket.tagDataList);
            bucket.pendingNasOuids.clear();
        }
    }
}
