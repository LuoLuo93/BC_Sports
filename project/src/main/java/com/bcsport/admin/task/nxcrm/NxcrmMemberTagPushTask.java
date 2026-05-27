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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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

    @Resource
    private NxcrmTagInfoMapper tagInfoMapper;

    @Resource
    private NxcrmTagValueMapper tagValueMapper;

    @Resource
    private NxcrmMemberTagDetailMapper memberTagDetailMapper;

    @Resource
    private NxCrmApiClient nxCrmApiClient;

    @Resource
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    public void pushMemberTags(Map<String, String> params) {
        log.info("=== 开始执行: NXCRM推送会员标签 ===");
        syncing = true;
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

            // 3. 查询会员标签数据

            List<NxcrmMemberTagDetail> allDetails = memberTagDetailMapper.selectList(
                new LambdaQueryWrapper<NxcrmMemberTagDetail>().orderByAsc(NxcrmMemberTagDetail::getNasOuid));
            if (allDetails.isEmpty()) {
                log.info("无会员标签数据");
                log.info("=== 完成执行: NXCRM推送会员标签, 成功=0, 失败=0 ===");
                return;
            }
            log.info("查询会员标签数据{}条", allDetails.size());

            // 4. 按nasOuid分组，构建每个会员的标签数据
            Map<String, List<NxcrmMemberTagDetail>> byNasOuid = allDetails.stream()
                .collect(Collectors.groupingBy(NxcrmMemberTagDetail::getNasOuid, LinkedHashMap::new, Collectors.toList()));

            Map<String, List<TagSetData>> nasOuidToTagDataList = new LinkedHashMap<>();
            int skipCount = 0;
            for (Map.Entry<String, List<NxcrmMemberTagDetail>> entry : byNasOuid.entrySet()) {
                String nasOuid = entry.getKey();
                Map<String, TagSetData> tagCodeToSetData = new LinkedHashMap<>();
                for (NxcrmMemberTagDetail detail : entry.getValue()) {
                    NxcrmTagInfo tagInfo = tagNameToInfo.get(detail.getTagName());
                    if (tagInfo == null) {
                        log.warn("标签名未找到对应tagCode: tagName={}, nasOuid={}", detail.getTagName(), nasOuid);
                        skipCount++;
                        continue;
                    }
                    TagSetData tsd = tagCodeToSetData.computeIfAbsent(tagInfo.getTagCode(), k -> {
                        TagSetData t = new TagSetData();
                        t.setTagCode(k);
                        return t;
                    });
                    if (detail.getTagValueName() != null) {
                        if (tsd.getTagValueList() == null) {
                            tsd.setTagValueList(new ArrayList<>());
                        }
                        tsd.getTagValueList().add(detail.getTagValueName());
                    } else {
                        skipCount++;
                        continue;
                    }
                    // 预定义值标签额外赋值 tagValueCodeList
                    if (tagInfo.getHasValue() != null && tagInfo.getHasValue() == 1) {
                        Map<String, String> valueNameMap = tagCodeToValueNameToCode.get(tagInfo.getTagCode());
                        if (valueNameMap != null && detail.getTagValueName() != null
                            && valueNameMap.containsKey(detail.getTagValueName())) {
                            if (tsd.getTagValueCodeList() == null) {
                                tsd.setTagValueCodeList(new ArrayList<>());
                            }
                            tsd.getTagValueCodeList().add(valueNameMap.get(detail.getTagValueName()));
                        }
                    }
                }
                List<TagSetData> validTags = tagCodeToSetData.values().stream()
                    .filter(t -> t.getTagValueList() != null && !t.getTagValueList().isEmpty())
                    .collect(Collectors.toList());
                if (!validTags.isEmpty()) {
                    nasOuidToTagDataList.put(nasOuid, validTags);
                    log.debug("nasOuid={}, 原始标签{}条, 匹配后{}条: {}", nasOuid, entry.getValue().size(), validTags.size(),
                        validTags.stream().map(t -> t.getTagCode() + "->" + t.getTagValueList())
                            .collect(Collectors.toList()));
                } else {
                    log.warn("nasOuid={}, 原始标签{}条, 全部匹配失败", nasOuid, entry.getValue().size());
                }
            }
            if (skipCount > 0) {
                log.warn("跳过无法映射的标签记录{}条", skipCount);
            }
            log.info("构建会员标签数据完成, 共{}个会员", nasOuidToTagDataList.size());

            // 5. 按标签组合分组（API对同一批memberIdList应用相同tagDataList）
            Map<String, List<String>> profileToNasOuids = new LinkedHashMap<>();
            Map<String, List<TagSetData>> profileToTagDataList = new LinkedHashMap<>();
            for (Map.Entry<String, List<TagSetData>> entry : nasOuidToTagDataList.entrySet()) {
                // 按tagCode排序确保JSON确定性
                entry.getValue().sort(Comparator.comparing(TagSetData::getTagCode));
                String profileKey = JSONUtil.toJsonStr(entry.getValue());
                profileToNasOuids.computeIfAbsent(profileKey, k -> new ArrayList<>()).add(entry.getKey());
                profileToTagDataList.putIfAbsent(profileKey, entry.getValue());
            }
            log.info("按标签组合分组完成, 共{}种组合", profileToNasOuids.size());

            // 6. 分批调用API
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            Semaphore semaphore = new Semaphore(API_CONCURRENCY);
            List<Runnable> batchTasks = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : profileToNasOuids.entrySet()) {
                String profileKey = entry.getKey();
                List<TagSetData> tagDataList = profileToTagDataList.get(profileKey);
                List<String> nasOuids = entry.getValue();

                for (int i = 0; i < nasOuids.size(); i += batchSize) {
                    List<String> batch = nasOuids.subList(i, Math.min(i + batchSize, nasOuids.size()));
                    batchTasks.add(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                nxCrmApiClient.multipleCustomerTagSet(batch, tagDataList);
                                successCount.addAndGet(batch.size());
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            failCount.addAndGet(batch.size());
                        } catch (Exception e) {
                            log.error("推送标签批次失败: {}", e.getMessage());
                            failCount.addAndGet(batch.size());
                        }
                    });
                }
            }

            if (batchTasks.isEmpty()) {
                log.info("无批次需要执行");
                log.info("=== 完成执行: NXCRM推送会员标签, 成功=0, 失败=0 ===");
                return;
            }

            CountDownLatch latch = new CountDownLatch(batchTasks.size());
            for (Runnable task : batchTasks) {
                taskThreadPool.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            log.info("=== 完成执行: NXCRM推送会员标签, 成功={}, 失败={} ===", successCount.get(), failCount.get());
        } catch (Exception e) {
            log.error("=== 失败执行: NXCRM推送会员标签 ===", e);
            throw new RuntimeException(e);
        } finally {
            syncing = false;
        }
    }
}
