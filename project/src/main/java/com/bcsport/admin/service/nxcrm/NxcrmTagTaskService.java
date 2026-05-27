package com.bcsport.admin.service.nxcrm;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.nxcrm.NxcrmMemberTagRecordQueryDTO;
import com.bcsport.admin.dto.nxcrm.NxcrmTagTaskDetailQueryDTO;
import com.bcsport.admin.dto.nxcrm.NxcrmTagTaskQueryDTO;
import com.bcsport.admin.entity.nxcrm.NxcrmMemberTag;
import com.bcsport.admin.entity.nxcrm.NxcrmTagTask;
import com.bcsport.admin.entity.nxcrm.NxcrmTagTaskDetail;
import com.bcsport.admin.mapper.nxcrm.NxcrmMemberTagMapper;
import com.bcsport.admin.mapper.nxcrm.NxcrmTagTaskDetailMapper;
import com.bcsport.admin.mapper.nxcrm.NxcrmTagTaskMapper;
import com.bcsport.admin.task.nxcrm.NxCrmApiClient;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.NxcrmMemberTagVO;
import com.bcsport.admin.vo.NxcrmTagTaskDetailVO;
import com.bcsport.admin.vo.NxcrmTagTaskVO;
import com.nascent.ecrp.opensdk.domain.customer.CustomerShopInfo;
import com.nascent.ecrp.opensdk.domain.customer.tag.TagSetData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NxcrmTagTaskService {

    private static final int API_CONCURRENCY = 3;
    private static final int BATCH_SIZE = 50;
    private static final int SHOP_QUERY_CONCURRENCY = 5;

    @Autowired
    private NxcrmTagTaskMapper taskMapper;

    @Autowired
    private NxcrmTagTaskDetailMapper detailMapper;

    @Autowired
    private NxcrmMemberTagMapper memberTagMapper;

    @Autowired
    private NxCrmApiClient nxCrmApiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    public NxcrmTagTaskVO getTaskById(String taskId) {
        NxcrmTagTask task = taskMapper.selectById(taskId);
        return BeanCopyUtils.copy(task, NxcrmTagTaskVO.class);
    }

    public String createTask(NxcrmTagTask task) {
        task.setStatus(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        taskMapper.insert(task);
        return task.getId();
    }

    public Result<?> executeTask(String taskId) {
        NxcrmTagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (task.getStatus() == 1) {
            return Result.error("任务正在执行中");
        }
        executeAsync(taskId);
        return Result.success("任务已触发");
    }

    public Result<?> getTaskStatus(String taskId) {
        NxcrmTagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("status", task.getStatus());
        data.put("successCount", task.getSuccessCount());
        data.put("failCount", task.getFailCount());
        data.put("totalCount", task.getTotalCount());
        return Result.success(data);
    }

    public PageResult<NxcrmTagTaskVO> pageTasks(PageQuery pageQuery, NxcrmTagTaskQueryDTO queryDTO) {
        LambdaQueryWrapper<NxcrmTagTask> wrapper = new LambdaQueryWrapper<NxcrmTagTask>()
            .orderByDesc(NxcrmTagTask::getCreateTime);
        Page<NxcrmTagTask> page = taskMapper.selectPage(pageQuery.toPage(), wrapper);
        return BeanCopyUtils.copyPage(PageResult.of(page), NxcrmTagTaskVO.class);
    }

    public PageResult<NxcrmTagTaskDetailVO> pageTaskDetails(String taskId, PageQuery pageQuery, NxcrmTagTaskDetailQueryDTO queryDTO) {
        LambdaQueryWrapper<NxcrmTagTaskDetail> wrapper = new LambdaQueryWrapper<NxcrmTagTaskDetail>()
            .eq(NxcrmTagTaskDetail::getTaskId, taskId)
            .orderByDesc(NxcrmTagTaskDetail::getCreateTime);
        if (queryDTO != null && queryDTO.getStatus() != null) {
            wrapper.eq(NxcrmTagTaskDetail::getStatus, queryDTO.getStatus());
        }
        Page<NxcrmTagTaskDetail> page = detailMapper.selectPage(pageQuery.toPage(), wrapper);
        return BeanCopyUtils.copyPage(PageResult.of(page), NxcrmTagTaskDetailVO.class);
    }

    public PageResult<NxcrmMemberTagVO> pageMemberTags(PageQuery pageQuery, NxcrmMemberTagRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<NxcrmMemberTag> wrapper = new LambdaQueryWrapper<NxcrmMemberTag>()
            .orderByDesc(NxcrmMemberTag::getCreateTime);
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getBatchNo())) {
                wrapper.eq(NxcrmMemberTag::getBatchNo, queryDTO.getBatchNo());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(NxcrmMemberTag::getStatus, queryDTO.getStatus());
            }
        }
        Page<NxcrmMemberTag> page = memberTagMapper.selectPage(pageQuery.toPage(), wrapper);
        return BeanCopyUtils.copyPage(PageResult.of(page), NxcrmMemberTagVO.class);
    }

    public void executeAsync(String taskId) {
        taskThreadPool.submit(() -> execute(taskId));
    }

    public void execute(String taskId) {
        NxcrmTagTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在: {}", taskId);
            return;
        }

        task.setStatus(1);
        taskMapper.updateById(task);

        List<NxcrmTagTaskDetail> details = detailMapper.selectList(
            new LambdaQueryWrapper<NxcrmTagTaskDetail>()
                .eq(NxcrmTagTaskDetail::getTaskId, taskId)
                .eq(NxcrmTagTaskDetail::getStatus, 0)
        );

        if (details.isEmpty()) {
            task.setStatus(2);
            task.setSuccessCount(0);
            task.setFailCount(0);
            taskMapper.updateById(task);
            return;
        }

        Map<String, List<NxcrmTagTaskDetail>> grouped = details.stream()
            .collect(Collectors.groupingBy(NxcrmTagTaskDetail::getTagDataJson));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Semaphore semaphore = new Semaphore(API_CONCURRENCY);

        for (Map.Entry<String, List<NxcrmTagTaskDetail>> entry : grouped.entrySet()) {
            List<NxcrmTagTaskDetail> group = entry.getValue();

            for (int i = 0; i < group.size(); i += BATCH_SIZE) {
                List<NxcrmTagTaskDetail> batch = group.subList(i, Math.min(i + BATCH_SIZE, group.size()));

                taskThreadPool.submit(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            doTagBatch(batch, entry.getKey());
                            successCount.addAndGet(batch.size());
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failCount.addAndGet(batch.size());
                    }
                });
            }
        }

        task.setSuccessCount(successCount.get());
        task.setFailCount(failCount.get());
        task.setStatus(2);
        taskMapper.updateById(task);
        log.info("南讯CRM打标签任务完成, taskId={}, 成功={}, 失败={}", taskId, successCount.get(), failCount.get());
    }

    private void doTagBatch(List<NxcrmTagTaskDetail> batch, String tagDataJson) {
        List<String> nasOuids = batch.stream()
            .map(d -> String.valueOf(d.getMemberId()))
            .collect(Collectors.toList());

        List<TagSetData> tagDataList = JSONUtil.toList(tagDataJson, TagSetData.class);

        try {
            nxCrmApiClient.multipleCustomerTagSet(nasOuids, tagDataList);
            for (NxcrmTagTaskDetail d : batch) {
                d.setStatus(1);
                detailMapper.updateById(d);
            }
        } catch (Exception e) {
            log.error("打标签批次失败: {}", e.getMessage());
            for (NxcrmTagTaskDetail d : batch) {
                d.setStatus(2);
                d.setErrorMsg(e.getMessage());
                detailMapper.updateById(d);
            }
        }
    }

    public int fillShopId(String batchNo) {
        List<NxcrmMemberTag> pending = memberTagMapper.selectList(
            new LambdaQueryWrapper<NxcrmMemberTag>()
                .eq(NxcrmMemberTag::getBatchNo, batchNo)
                .eq(NxcrmMemberTag::getStatus, 0)
                .isNull(NxcrmMemberTag::getShopId)
        );

        int updated = 0;
        Semaphore semaphore = new Semaphore(SHOP_QUERY_CONCURRENCY);

        for (NxcrmMemberTag item : pending) {
            try {
                semaphore.acquire();
                List<CustomerShopInfo> shops = nxCrmApiClient.getShopByCustomer(item.getNasOuid());
                if (shops != null && !shops.isEmpty()) {
                    CustomerShopInfo shop = shops.get(0);
                    item.setShopId(shop.getShopId());
                    item.setShopName(shop.getShopName());
                    item.setStatus(1);
                } else {
                    item.setStatus(3);
                    item.setErrorMsg("未查询到店铺信息");
                }
            } catch (Exception e) {
                item.setStatus(3);
                item.setErrorMsg(e.getMessage());
            } finally {
                semaphore.release();
                memberTagMapper.updateById(item);
                updated++;
            }
        }
        log.info("shopId填充完成, batchNo={}, 更新{}条", batchNo, updated);
        return updated;
    }

    public void fillAllPendingShopId() {
        List<NxcrmMemberTag> pending = memberTagMapper.selectList(
            new LambdaQueryWrapper<NxcrmMemberTag>()
                .select(NxcrmMemberTag::getBatchNo)
                .eq(NxcrmMemberTag::getStatus, 0)
                .isNull(NxcrmMemberTag::getShopId)
                .groupBy(NxcrmMemberTag::getBatchNo)
        );
        for (NxcrmMemberTag item : pending) {
            fillShopId(item.getBatchNo());
        }
    }
}
