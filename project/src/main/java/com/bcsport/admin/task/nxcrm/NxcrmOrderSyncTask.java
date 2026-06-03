package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.entity.ihr.NanXOrderDetail;
import com.bcsport.admin.entity.ihr.NanXOrderMaster;
import com.bcsport.admin.ihrmapper.NanXOrderMapper;
import com.nascent.ecrp.opensdk.domain.trade.OrderDetailVo;
import com.nascent.ecrp.opensdk.domain.trade.TradeDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component("nxcrmOrderSyncTask")
public class NxcrmOrderSyncTask {

    private static final int ORDER_BATCH_SIZE = 100;
    private static final int ORDER_PAGE_SIZE = 1000;
    private static final int MAX_RETRY = 3;

    @Autowired
    private NanXOrderMapper nanXOrderMapper;

    @Autowired
    private NxCrmApiClient nxCrmApiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    public void syncOrders() {
        log.info("=== 开始执行: 南讯CRM同步订单 ===");
        try {
            doSyncOrders();
            log.info("=== 完成执行: 南讯CRM同步订单 ===");
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM同步订单 ===", e);
            throw e;
        }
    }

    private static final String SHOP_ID = "BCHY";

    private void doSyncOrders() {
        log.info("订单同步开始, 全量模式, shopId={}", SHOP_ID);

        AtomicInteger totalSynced = new AtomicInteger(0);
        AtomicInteger totalFailed = new AtomicInteger(0);
        List<Runnable> batchTasks = new ArrayList<>();
        int offset = 0;

        while (true) {
            List<NanXOrderMaster> orders;
            try {
                orders = nanXOrderMapper.selectAllOrdersPaged(offset, ORDER_PAGE_SIZE);
            } catch (Exception e) {
                log.error("查询订单失败, offset={}: {}", offset, e.getMessage(), e);
                break;
            }
            if (orders == null || orders.isEmpty()) {
                break;
            }

            List<TradeDetailVo> converted = orders.stream()
                .map(NxcrmOrderSyncTask::toTradeDetailVo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            for (int i = 0; i < converted.size(); i += ORDER_BATCH_SIZE) {
                List<TradeDetailVo> batch = new ArrayList<>(
                    converted.subList(i, Math.min(i + ORDER_BATCH_SIZE, converted.size())));
                batchTasks.add(() -> {
                    try {
                        boolean ok = retrySave(batch, SHOP_ID);
                        if (ok) {
                            totalSynced.addAndGet(batch.size());
                        } else {
                            totalFailed.addAndGet(batch.size());
                        }
                    } catch (Exception e) {
                        log.error("批次提交异常: {}", e.getMessage(), e);
                        totalFailed.addAndGet(batch.size());
                    }
                });
            }

            if (orders.size() < ORDER_PAGE_SIZE) {
                break;
            }
            offset += ORDER_PAGE_SIZE;
        }

        if (batchTasks.isEmpty()) {
            log.info("无订单需要同步");
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

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单同步被中断");
        }

        log.info("订单同步完成, 成功={}, 失败={}", totalSynced.get(), totalFailed.get());
    }

    private boolean retrySave(List<TradeDetailVo> batch, String shopId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                nxCrmApiClient.saveOrders(batch, shopId);
                return true;
            } catch (Exception e) {
                log.warn("门店{}批次同步失败, 第{}次重试, 批次{}条: {}", shopId, attempt, batch.size(), e.getMessage());
                if (attempt == MAX_RETRY) {
                    log.error("门店{}批次同步最终失败, 放弃{}条", shopId, batch.size());
                    return false;
                }
            }
        }
        return false;
    }

    private static TradeDetailVo toTradeDetailVo(NanXOrderMaster master) {
        if (master == null) return null;
        TradeDetailVo vo = new TradeDetailVo();
        vo.setTotalFee(master.getTotalFee());
        vo.setPayment(master.getPayment());
        vo.setShippingType("free");
        vo.setOutTradeId(master.getOutTradeId());
        vo.setNasOuid(master.getNasOuid());
        vo.setTradeStatus("TRADE_FINISHED");
        vo.setTradeType("fixed");
        vo.setTradeFrom("OFFLINE");
        vo.setCreated(master.getCreated());
        vo.setNum(master.getNum());
        vo.setPayTime(master.getCreated());
        vo.setConsignTime(master.getCreated());
        vo.setEndTime(master.getCreated());
        vo.setRefundStatus(0);
        vo.setPayType(4);
        vo.setIsBind(true);
        vo.setIsCalIntegral(false);
        if (master.getDetails() != null && !master.getDetails().isEmpty()) {
            List<OrderDetailVo> details = master.getDetails().stream()
                .map(NxcrmOrderSyncTask::toOrderDetailVo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            vo.setOrderDetailVoList(details);
        }
        return vo;
    }

    private static OrderDetailVo toOrderDetailVo(NanXOrderDetail detail) {
        if (detail == null) return null;
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOutOrderId(detail.getOutOrderId());
        vo.setOrderStatus("TRADE_FINISHED");
        vo.setTitle(detail.getTitle());
        vo.setOutItemId(detail.getOutItemId());
        vo.setOuterId(detail.getOutItemId());
        vo.setSkuId(detail.getOuterSkuId());
        vo.setOuterSkuId(detail.getOuterSkuId());
        vo.setOrderNum(detail.getOrderNum());
        vo.setOrderTotalFee(detail.getOrderTotalFee());
        vo.setOrderPayment(detail.getOrderPayment());
        vo.setOrderPrice(detail.getOrderPrice());
        return vo;
    }
}
