package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.entity.ihr.NxcrmUnbindQueue;
import com.bcsport.admin.ihrmapper.NxcrmUnbindQueueMapper;
import com.bcsport.admin.task.nxcrm.NxCrmApiClient.UnbindResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 南讯 CRM 会员解绑定时任务。
 *
 * <p>数据源：{@code BC_SPORTS_IHR.NxcrmUnbindQueue}（业务侧插入待解绑记录）。
 *
 * <p>执行模型：
 * <ul>
 *   <li>分页扫描 status=0 的记录</li>
 *   <li>每页内通过 taskThreadPool 并发执行，Semaphore(5) 限制 API 并发度避免触发南讯限流</li>
 *   <li>每条记录单次调用 unbindCustomer，最多重试 3 次</li>
 *   <li>成功 → 回写 status=1；最终失败 → 回写 status=2 + errorMsg（不阻塞后续记录）</li>
 *   <li>每页 await 完成后再处理下一页，避免一次性提交过多任务撑爆线程池队列</li>
 * </ul>
 *
 * <p>每阶段整体等待最多 30 分钟，超时则中断当前页处理并继续。
 */
@Slf4j
@Component("nxcrmUnbindTask")
public class NxcrmUnbindTask {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RETRY = 3;
    private static final int API_CONCURRENCY = 5;
    private static final long PAGE_AWAIT_MINUTES = 30L;
    /** 单条 errorMsg 字段最大长度，超长截断避免数据库列溢出 */
    private static final int ERROR_MSG_MAX_LEN = 500;

    @Autowired
    private NxcrmUnbindQueueMapper unbindQueueMapper;

    @Autowired
    private NxCrmApiClient nxCrmApiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    /**
     * 定时任务入口（无参，由 ScheduleTaskRegistry 反射调用）。
     */
    public void unbindMembers() {
        log.info("=== 开始执行: 南讯CRM会员解绑 ===");
        try {
            doUnbind();
            log.info("=== 完成执行: 南讯CRM会员解绑 ===");
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM会员解绑 ===", e);
            throw e;
        }
    }

    private void doUnbind() {
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalFailed  = new AtomicInteger(0);
        Semaphore semaphore = new Semaphore(API_CONCURRENCY);

        while (true) {
            // 始终从 offset=0 捞：成功的记录 status→1 离开结果集，自然前移
            List<NxcrmUnbindQueue> pending;
            try {
                pending = unbindQueueMapper.selectPendingPaged(0, PAGE_SIZE);
            } catch (Exception e) {
                log.error("查询待解绑记录失败: {}", e.getMessage(), e);
                break;
            }
            if (pending == null || pending.isEmpty()) {
                break;
            }
            log.info("待解绑记录本页{}条", pending.size());

            CountDownLatch latch = new CountDownLatch(pending.size());
            for (NxcrmUnbindQueue item : pending) {
                taskThreadPool.submit(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            processOne(item, totalSuccess, totalFailed);
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("解绑任务被中断, id={}, nasOuid={}", item.getId(), item.getNasOuid());
                        totalFailed.incrementAndGet();
                    } catch (Exception e) {
                        // processOne 内部已吞异常，这里仅防御性兜底
                        log.error("解绑记录处理异常, id={}: {}", item.getId(), e.getMessage(), e);
                        totalFailed.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                boolean done = latch.await(PAGE_AWAIT_MINUTES, TimeUnit.MINUTES);
                if (!done) {
                    log.error("本页解绑等待超时({}分钟), 继续下一页", PAGE_AWAIT_MINUTES);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("解绑任务被中断");
                break;
            }

            // 本页全部处理完后，下一轮 while 从 offset=0 重新捞
            // 成功记录已离开结果集，失败记录留在结果集但 retryCount 已累加
        }

        log.info("会员解绑完成, 成功={}, 失败={}", totalSuccess.get(), totalFailed.get());
    }

    /**
     * 处理单条解绑记录：调用接口 → 判断结果 → 状态回写。
     *
     * <p>结果判定：
     * <ul>
     *   <li>response.success=true → DONE，记录 [成功] code + msg</li>
     *   <li>response.success=false → FAILED（业务失败，不重试），记录 [失败] code + msg</li>
     *   <li>系统异常（已重试 MAX_RETRY 次）→ FAILED，记录 [系统异常] 异常信息</li>
     * </ul>
     */
    private void processOne(NxcrmUnbindQueue item, AtomicInteger totalSuccess, AtomicInteger totalFailed) {
        UnbindResult result = callUnbindWithRetry(item.getNasOuid(), item.getShopId());
        Date now = new Date();
        int prevRetry = item.getRetryCount() == null ? 0 : item.getRetryCount();

        try {
            String msgToStore = truncate(formatErrorMsg(result), ERROR_MSG_MAX_LEN);

            if (result.status == UnbindResult.STATUS_OK) {
                unbindQueueMapper.updateStatusById(item.getId(),
                    NxcrmUnbindQueue.STATUS_DONE,
                    prevRetry,
                    now,
                    msgToStore);
                totalSuccess.incrementAndGet();
            } else {
                // 业务失败 和 系统异常 都累加 retryCount（避免无限重试）
                // 业务失败累加 1（只试了 1 次就确定是数据问题）
                // 系统异常累加 MAX_RETRY（已重试了 MAX_RETRY 次）
                int retryInc = (result.status == UnbindResult.STATUS_SYSTEM_ERROR) ? MAX_RETRY : 1;
                unbindQueueMapper.updateStatusById(item.getId(),
                    NxcrmUnbindQueue.STATUS_FAILED,
                    prevRetry + retryInc,
                    now,
                    msgToStore);
                totalFailed.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("回写解绑状态失败, id={}, nasOuid={}: {}", item.getId(), item.getNasOuid(), e.getMessage(), e);
            totalFailed.incrementAndGet();
        }
    }

    /**
     * 格式化可读的 errorMsg 字符串（含接口原始返回内容）。
     */
    private String formatErrorMsg(UnbindResult result) {
        switch (result.status) {
            case UnbindResult.STATUS_OK:
                return "[成功] code=" + result.response.code
                    + ", msg=" + result.response.msg
                    + ", result=" + result.response.result
                    + ", response=" + result.response.rawBody;
            case UnbindResult.STATUS_BIZ_FAIL:
                return "[业务失败] code=" + result.response.code
                    + ", msg=" + result.response.msg
                    + ", response=" + result.response.rawBody;
            case UnbindResult.STATUS_SYSTEM_ERROR:
            default:
                return "[系统异常] " + result.fallbackMsg;
        }
    }

    /**
     * 调用解绑接口（带系统异常重试）。
     *
     * <p>重试逻辑：
     * <ul>
     *   <li>API 返回 response（无论 success=true/false）→ 立即返回，不重试（业务结果已确定）</li>
     *   <li>SDK 抛异常（网络超时等系统错误）→ 重试最多 MAX_RETRY 次</li>
     * </ul>
     */
    private UnbindResult callUnbindWithRetry(String nasOuid, String shopId) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                UnbindResponse resp = nxCrmApiClient.unbindCustomer(nasOuid, shopId);
                if (resp.success) {
                    return UnbindResult.ok(resp);
                } else {
                    // 业务失败：不重试（如"会员不存在"重试也没用）
                    return UnbindResult.bizFail(resp);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("门店{}会员解绑系统异常(第{}/{}次尝试), nasOuid={}: {}",
                    shopId, attempt, MAX_RETRY, nasOuid, e.getMessage());
            }
        }
        String fallbackMsg = (lastException != null) ? lastException.getMessage() : "未知错误";
        log.error("门店{}会员解绑系统异常(已尝试{}次), nasOuid={}: {}",
            shopId, MAX_RETRY, nasOuid, fallbackMsg);
        return UnbindResult.systemError(fallbackMsg);
    }

    private static final class UnbindResult {
        static final int STATUS_OK           = 0;  // 业务成功
        static final int STATUS_BIZ_FAIL     = 1;  // 业务失败（不重试）
        static final int STATUS_SYSTEM_ERROR = 2;  // 系统异常（已重试）

        final int status;
        final UnbindResponse response;  // STATUS_OK / STATUS_BIZ_FAIL 时有值
        final String fallbackMsg;       // STATUS_SYSTEM_ERROR 时有值

        UnbindResult(int status, UnbindResponse response, String fallbackMsg) {
            this.status = status;
            this.response = response;
            this.fallbackMsg = fallbackMsg;
        }

        static UnbindResult ok(UnbindResponse resp) {
            return new UnbindResult(STATUS_OK, resp, null);
        }
        static UnbindResult bizFail(UnbindResponse resp) {
            return new UnbindResult(STATUS_BIZ_FAIL, resp, null);
        }
        static UnbindResult systemError(String msg) {
            return new UnbindResult(STATUS_SYSTEM_ERROR, null, msg);
        }
    }

    private static String truncate(String msg, int maxLen) {
        if (msg == null) return null;
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}
