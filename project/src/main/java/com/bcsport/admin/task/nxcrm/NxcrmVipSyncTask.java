package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.entity.ihr.EzrVipInfo;
import com.bcsport.admin.ihrmapper.EzrVipInfoMapper;
import com.nascent.ecrp.opensdk.domain.customer.CustomerGradeUpdateInfo;
import com.nascent.ecrp.opensdk.domain.customer.CustomerSaveInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 南讯 CRM 会员同步定时任务（基本信息 + 等级）。
 *
 * <p>数据流：BC_SPORTS_IHR.NXVipInfo → 转换 → 南讯 BatchCustomerSave / CustomerGradeUpdate 接口。
 * <p>对应原 interfaceForYZ 项目中的 NanXCrmVipController#syncVipsAndGrades 方法。
 *
 * <p><b>执行模型（单店串行 + 店内并发）</b>：按门店逐个处理。
 * 每个门店内部：分页扫描构建批次 → 阶段一并发注册 → 守卫 → 阶段二并发改等级。
 * 阶段一二在门店级别严格串行，保证「先注册后改等级」的业务约束。
 *
 * <p><b>内存</b>：单店数据通常 ≤ 10 万，批次缓存峰值 ≤ 100MB。
 * 50 万级总会员不会全部驻留内存。
 *
 * <p><b>守卫</b>：阶段一某门店存在失败 → 该门店阶段二跳过，其他门店继续。
 *
 * <p><b>超时保护</b>：每个阶段最多等待 30 分钟，超时不再阻塞主线程。
 */
@Slf4j
@Component("nxcrmVipSyncTask")
public class NxcrmVipSyncTask {

    private static final int VIP_PAGE_SIZE = 1000;
    private static final int VIP_BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;
    private static final long PHASE_AWAIT_MINUTES = 30L;

    @Autowired
    private EzrVipInfoMapper ezrVipInfoMapper;

    @Autowired
    private NxCrmApiClient nxCrmApiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    /** 批次上下文：携带 shopId 与具体负载 */
    private static final class BatchTask<T> {
        final String shopId;
        final List<T> payload;
        BatchTask(String shopId, List<T> payload) {
            this.shopId = shopId;
            this.payload = payload;
        }
    }

    /** 全局累加计数器（跨门店） */
    private static final class GlobalCounters {
        final AtomicInteger customerSuccess = new AtomicInteger(0);
        final AtomicInteger customerFailed  = new AtomicInteger(0);
        final AtomicInteger gradeSuccess    = new AtomicInteger(0);
        final AtomicInteger gradeFailed     = new AtomicInteger(0);
        final AtomicInteger gradeSkipped    = new AtomicInteger(0);  // 因阶段一失败而跳过的等级更新条数
        final AtomicInteger shopsSkipped    = new AtomicInteger(0);  // 因阶段一失败而跳过的门店数
    }

    /**
     * 定时任务入口（无参，由 ScheduleTaskRegistry 反射调用）。
     */
    public void syncVips() {
        log.info("=== 开始执行: 南讯CRM同步会员(基本信息+等级) ===");
        try {
            doSyncVips();
            log.info("=== 完成执行: 南讯CRM同步会员(基本信息+等级) ===");
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM同步会员(基本信息+等级) ===", e);
            throw e;
        }
    }

    private void doSyncVips() {
        // === 1. 拉取门店列表 ===
        List<String> shopList;
        try {
            shopList = ezrVipInfoMapper.selectDistinctShopList();
        } catch (Exception e) {
            log.error("查询门店列表失败: {}", e.getMessage(), e);
            return;
        }
        if (shopList == null || shopList.isEmpty()) {
            log.warn("无待同步门店");
            return;
        }
        log.info("待同步门店{}个: {}", shopList.size(),
            shopList.size() > 10 ? shopList.subList(0, 10) + "...(more)" : shopList);

        GlobalCounters counters = new GlobalCounters();

        // === 2. 逐店处理 ===
        for (String shopId : shopList) {
            try {
                syncOneShop(shopId, counters);
            } catch (Exception e) {
                // 单店整体异常不影响其他店
                log.error("门店{}同步整体异常, 继续处理下一个门店: {}", shopId, e.getMessage(), e);
            }
        }

        // === 3. 全局汇总 ===
        log.info("会员同步全部完成, 基本信息[成功={}, 失败={}], 等级更新[成功={}, 失败={}, 跳过{}条/{}个门店]",
            counters.customerSuccess.get(), counters.customerFailed.get(),
            counters.gradeSuccess.get(), counters.gradeFailed.get(),
            counters.gradeSkipped.get(), counters.shopsSkipped.get());
    }

    /**
     * 处理单个门店：扫描 → 阶段一 → 守卫 → 阶段二。
     */
    private void syncOneShop(String shopId, GlobalCounters counters) {
        // === 1. 扫描该店，构建两类批次 ===
        List<BatchTask<CustomerSaveInfo>> customerBatches = new ArrayList<>();
        List<BatchTask<CustomerGradeUpdateInfo>> gradeBatches = new ArrayList<>();

        int offset = 0;
        while (true) {
            List<EzrVipInfo> vips;
            try {
                vips = ezrVipInfoMapper.selectEzrVipInfoPaged(shopId, offset, VIP_PAGE_SIZE);
            } catch (Exception e) {
                log.error("查询会员失败, shopId={}, offset={}: {}", shopId, offset, e.getMessage(), e);
                break;
            }
            if (vips == null || vips.isEmpty()) {
                break;
            }

            // 基本信息批次
            List<CustomerSaveInfo> customerList = vips.stream()
                .map(EzrVipConverter::toCustomerSaveInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            for (int i = 0; i < customerList.size(); i += VIP_BATCH_SIZE) {
                List<CustomerSaveInfo> batch = new ArrayList<>(
                    customerList.subList(i, Math.min(i + VIP_BATCH_SIZE, customerList.size())));
                customerBatches.add(new BatchTask<>(shopId, batch));
            }

            // 等级更新批次（同一份 vips，避免重复查表）
            List<CustomerGradeUpdateInfo> gradeList = vips.stream()
                .map(EzrVipConverter::toGradeUpdateInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            for (int i = 0; i < gradeList.size(); i += VIP_BATCH_SIZE) {
                List<CustomerGradeUpdateInfo> batch = new ArrayList<>(
                    gradeList.subList(i, Math.min(i + VIP_BATCH_SIZE, gradeList.size())));
                gradeBatches.add(new BatchTask<>(shopId, batch));
            }

            if (vips.size() < VIP_PAGE_SIZE) {
                break;
            }
            offset += VIP_PAGE_SIZE;
        }

        if (customerBatches.isEmpty() && gradeBatches.isEmpty()) {
            log.warn("门店{}无会员数据", shopId);
            return;
        }
        log.info("门店{}构建批次完成: 基本信息{}批, 等级更新{}批",
            shopId, customerBatches.size(), gradeBatches.size());

        // === 2. 阶段一：并发注册基本信息 ===
        PhaseResult customerResult = runPhase("基本信息", customerBatches,
            (task, ok, fail) -> {
                int successCount = retrySave(task.payload, task.shopId);
                int failedCount  = task.payload.size() - successCount;
                ok.addAndGet(successCount);
                fail.addAndGet(failedCount);
            });

        counters.customerSuccess.addAndGet(customerResult.success.get());
        counters.customerFailed.addAndGet(customerResult.failed.get());

        // === 3. 守卫：阶段一失败 → 跳过该店阶段二 ===
        if (customerResult.failed.get() > 0) {
            int skipped = gradeBatches.stream().mapToInt(b -> b.payload.size()).sum();
            counters.gradeSkipped.addAndGet(skipped);
            counters.shopsSkipped.incrementAndGet();
            log.warn("门店{}基本信息阶段存在失败[失败={}条], 跳过等级更新阶段, 涉及{}条等级更新",
                shopId, customerResult.failed.get(), skipped);
            return;
        }

        // === 4. 阶段二：并发更新等级 ===
        PhaseResult gradeResult = runPhase("等级更新", gradeBatches,
            (task, ok, fail) -> {
                int successCount = retrySaveGrade(task.payload, task.shopId);
                ok.addAndGet(successCount);
                fail.addAndGet(task.payload.size() - successCount);
            });

        counters.gradeSuccess.addAndGet(gradeResult.success.get());
        counters.gradeFailed.addAndGet(gradeResult.failed.get());
    }

    /**
     * 通用阶段执行器：并发提交 + CountDownLatch 等待（带超时）。
     * 超时不强行中断已提交批次（避免半提交状态），仅记录未完成数。
     */
    private <T> PhaseResult runPhase(String phaseName,
                                     List<BatchTask<T>> batches,
                                     PhaseRunner<T> runner) {
        PhaseResult result = new PhaseResult();
        if (batches.isEmpty()) {
            log.info("阶段[{}]无批次需要执行", phaseName);
            return result;
        }
        log.info("阶段[{}]开始, 共{}个批次", phaseName, batches.size());

        CountDownLatch latch = new CountDownLatch(batches.size());
        for (BatchTask<T> task : batches) {
            taskThreadPool.submit(() -> {
                try {
                    runner.run(task, result.success, result.failed);
                } catch (Exception e) {
                    // runner 内部 retry 已吞异常，理论上不会走到这里；防御性兜底
                    log.error("阶段[{}]批次提交异常, shopId={}: {}",
                        phaseName, task.shopId, e.getMessage(), e);
                    result.failed.addAndGet(task.payload.size());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean done = latch.await(PHASE_AWAIT_MINUTES, TimeUnit.MINUTES);
            if (!done) {
                long pending = latch.getCount();
                log.error("阶段[{}]等待超时({}分钟), 仍有{}个批次未完成（统计未计入失败）",
                    phaseName, PHASE_AWAIT_MINUTES, pending);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("阶段[{}]被中断", phaseName);
        }
        log.info("阶段[{}]完成, 成功={}, 失败={}",
            phaseName, result.success.get(), result.failed.get());
        return result;
    }

    /**
     * 重试封装：基本信息。
     * 全批次异常时重试，部分失败不重试（数据问题，重试无益）。
     * 返回真实成功条数（saveCustomers 基于 customerSaveFailedList 计算）。
     */
    private int retrySave(List<CustomerSaveInfo> batch, String shopId) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return nxCrmApiClient.saveCustomers(batch, shopId);
            } catch (Exception e) {
                lastException = e;
                // M4: 语义改为"第 X/Y 次尝试"，避免"第 1 次重试"实为首次尝试的歧义
                log.warn("门店{}会员基本信息批次同步失败(第{}/{}次尝试), 批次{}条: {}",
                    shopId, attempt, MAX_RETRY, batch.size(), e.getMessage());
            }
        }
        if (lastException != null) {
            log.error("门店{}会员基本信息批次同步最终失败(已尝试{}次), 放弃{}条: {}",
                shopId, MAX_RETRY, batch.size(), lastException.getMessage());
        }
        return 0;
    }

    /**
     * 重试封装：等级更新。
     */
    private int retrySaveGrade(List<CustomerGradeUpdateInfo> batch, String shopId) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return nxCrmApiClient.updateCustomerGrades(batch, shopId);
            } catch (Exception e) {
                lastException = e;
                log.warn("门店{}会员等级批次同步失败(第{}/{}次尝试), 批次{}条: {}",
                    shopId, attempt, MAX_RETRY, batch.size(), e.getMessage());
            }
        }
        if (lastException != null) {
            log.error("门店{}会员等级批次同步最终失败(已尝试{}次), 放弃{}条: {}",
                shopId, MAX_RETRY, batch.size(), lastException.getMessage());
        }
        return 0;
    }

    @FunctionalInterface
    private interface PhaseRunner<T> {
        void run(BatchTask<T> task, AtomicInteger ok, AtomicInteger fail);
    }

    private static final class PhaseResult {
        final AtomicInteger success = new AtomicInteger(0);
        final AtomicInteger failed  = new AtomicInteger(0);
    }
}
