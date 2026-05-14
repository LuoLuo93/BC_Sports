package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.QywxAttrsBase;
import com.bcsport.admin.entity.qywx.QywxDepartmentMember;
import com.bcsport.admin.qywxmapper.QywxAttrsBaseMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业微信任务：同步成员扩展属性
 *
 * 功能：从企业微信API获取成员的extattr.attrs并保存到VX_attrsBase表
 * 对应原接口项目中的 VX_attrsBase 同步逻辑
 *
 * 优化：采用分批处理模式，避免内存溢出（OOM）
 * 每处理一批用户就立即写入数据库，然后释放内存
 */
@Slf4j
@Component("qywxAttrsBaseTask")
public class QywxAttrsBaseTask {

    private static final int CONCURRENT_THREADS = 10;
    private static final long THREAD_POOL_TIMEOUT_SECONDS = 300;

    /**
     * 每批处理的用户数 - 调小此值可降低内存峰值
     */
    private static final int USER_BATCH_SIZE = 500;

    /**
     * 每批写入的属性记录数
     */
    private static final int ATTRS_BATCH_SIZE = 50;

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxDepartmentMemberMapper departmentMemberMapper;

    @Autowired
    private QywxAttrsBaseMapper attrsBaseMapper;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    /**
     * 用户扩展属性处理结果
     */
    private static class AttrsResult {
        String userid;
        List<QywxAttrsBase> attrsList;
        boolean success;
        String errorMsg;

        AttrsResult(String userid, boolean success, String errorMsg) {
            this.userid = userid;
            this.success = success;
            this.errorMsg = errorMsg;
        }

        AttrsResult(String userid, List<QywxAttrsBase> attrsList) {
            this.userid = userid;
            this.attrsList = attrsList;
            this.success = true;
        }
    }

    /**
     * 同步成员扩展属性
     *
     * 优化版：先获取所有数据（API调用在事务外），再在短事务中写入数据库
     */
    public void sync() {
        log.info("=== 开始执行: 同步企微成员扩展属性 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取所有需要同步的userid（API调用：事务外）
            List<String> userIds = getUserIdsForSync();

            if (userIds == null || userIds.isEmpty()) {
                log.warn("=== 完成: 无成员数据 ===");
                return;
            }

            log.info("共 {} 个成员需要同步扩展属性", userIds.size());

            // 2. 并发获取所有属性数据（API调用：事务外）
            List<QywxAttrsBase> allAttrsList = new ArrayList<>();
            int totalSuccessCount = 0;
            int totalFailCount = 0;

            List<List<String>> userBatches = partitionList(userIds, USER_BATCH_SIZE);

            for (int batchIndex = 0; batchIndex < userBatches.size(); batchIndex++) {
                List<String> batchUserIds = userBatches.get(batchIndex);

                // 处理当前批次（API调用，不含DB写入）
                BatchResult batchResult = processBatch(batchUserIds, batchIndex, userBatches.size());

                // 收集属性数据
                allAttrsList.addAll(batchResult.attrsList);

                totalSuccessCount += batchResult.successCount;
                totalFailCount += batchResult.failCount;
            }

            // 3. DB writes: short transaction
            new TransactionTemplate(transactionManager).execute(status -> {
                attrsBaseMapper.deleteAll();

                if (!allAttrsList.isEmpty()) {
                    batchInsertAttrs(allAttrsList);
                }
                return null;
            });

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== 完成: 同步企微成员扩展属性, 成功: {}, 失败: {}, 属性: {}, 耗时: {} ms ===",
                    totalSuccessCount, totalFailCount, allAttrsList.size(), totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微成员扩展属性 ===", e);
            throw e;
        }
    }

    /**
     * 单个批次处理结果
     */
    private static class BatchResult {
        int successCount;
        int failCount;
        int attrsCount;
        List<QywxAttrsBase> attrsList;

        BatchResult(int successCount, int failCount, int attrsCount, List<QywxAttrsBase> attrsList) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.attrsCount = attrsCount;
            this.attrsList = attrsList;
        }
    }

    /**
     * 处理一个用户批次（仅API调用，不写DB）
     */
    private BatchResult processBatch(List<String> userIds, int batchIndex, int totalBatches) {
        // 并发获取当前批次用户的扩展属性
        List<AttrsResult> results = fetchAttrsConcurrently(userIds, batchIndex, totalBatches);

        // 收集当前批次的属性
        List<QywxAttrsBase> batchAttrsList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (AttrsResult result : results) {
            if (result.success && result.attrsList != null) {
                batchAttrsList.addAll(result.attrsList);
                successCount++;
            } else {
                failCount++;
            }
        }

        return new BatchResult(successCount, failCount, batchAttrsList.size(), batchAttrsList);
    }

    /**
     * 将列表分割成指定大小的小批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, end));
        }
        return batches;
    }

    /**
     * 获取需要同步的userid列表
     * 优先从本地表获取，如果没有则从API获取
     */
    private List<String> getUserIdsForSync() {
        List<String> userIds = departmentMemberMapper.selectAllUserIds();

        if (userIds == null || userIds.isEmpty()) {
            log.info("本地无部门成员数据, 从API获取...");
            List<QywxDepartmentMember> members = apiClient.getDepartmentListAll();
            if (members != null && !members.isEmpty()) {
                userIds = new ArrayList<>();
                for (QywxDepartmentMember member : members) {
                    if (member.getUserid() != null) {
                        userIds.add(member.getUserid());
                    }
                }
            }
        }

        return userIds;
    }

    /**
     * 并发获取用户扩展属性（使用统一线程池）
     */
    private List<AttrsResult> fetchAttrsConcurrently(List<String> userIds, int batchIndex, int totalBatches) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Fetching attrs for {} users in batch {}/{} using shared thread pool...",
                userIds.size(), batchIndex + 1, totalBatches);
        long fetchStart = System.currentTimeMillis();

        List<Future<AttrsResult>> futures = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(userIds.size());
        List<AttrsResult> results = Collections.synchronizedList(new ArrayList<>());

        try {
            for (String userid : userIds) {
                taskThreadPool.submit(() -> {
                    try {
                        AttrsResult result = fetchSingleUserAttrs(userid);
                        results.add(result);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }

            // 等待所有任务完成
            try {
                countDownLatch.await(THREAD_POOL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Task interrupted", e);
            }

            long fetchTime = System.currentTimeMillis() - fetchStart;
            log.debug("Fetched attrs for {} users in {}ms (batch {}/{})",
                    userIds.size(), fetchTime, batchIndex + 1, totalBatches);
            return results;

        } catch (Exception e) {
            log.error("Error fetching attrs", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取单个用户的扩展属性
     */
    private AttrsResult fetchSingleUserAttrs(String userid) {
        try {
            JSONObject userDetail = apiClient.getUserDetail(userid);
            if (userDetail == null) {
                return new AttrsResult(userid, false, "User detail is null");
            }

            // 检查返回是否有错误
            Integer errcode = userDetail.getInt("errcode");
            if (errcode != null && errcode != 0) {
                return new AttrsResult(userid, false,
                        "errcode: " + errcode + ", errmsg: " + userDetail.getStr("errmsg"));
            }

            // 提取 extattr.attrs
            List<QywxAttrsBase> attrsList = extractAttrs(userid, userDetail);

            return new AttrsResult(userid, attrsList);

        } catch (Exception e) {
            log.warn("Exception for userid: {}", userid, e);
            return new AttrsResult(userid, false, e.getMessage());
        }
    }

    /**
     * 从用户详情JSON中提取扩展属性
     */
    private List<QywxAttrsBase> extractAttrs(String userid, JSONObject userDetail) {
        List<QywxAttrsBase> result = new ArrayList<>();

        JSONObject extattr = userDetail.getJSONObject("extattr");
        if (extattr == null) {
            return result;
        }

        JSONArray attrs = extattr.getJSONArray("attrs");
        if (attrs == null || attrs.size() == 0) {
            return result;
        }

        for (int i = 0; i < attrs.size(); i++) {
            JSONObject attrObj = attrs.getJSONObject(i);
            QywxAttrsBase attr = new QywxAttrsBase();
            attr.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
            attr.setUserid(userid);
            attr.setName(attrObj.getStr("name"));
            attr.setType(attrObj.getStr("type"));

            // 兼容两种格式：直接有value，或者在text.value中
            String value = attrObj.getStr("value");
            if (value == null || value.isEmpty()) {
                JSONObject text = attrObj.getJSONObject("text");
                if (text != null) {
                    value = text.getStr("value");
                }
            }
            attr.setEnrollInDate(value);

            attr.setCreateTime(LocalDateTime.now());
            result.add(attr);
        }

        return result;
    }

    /**
     * 批量插入扩展属性
     */
    private void batchInsertAttrs(List<QywxAttrsBase> attrsList) {
        log.debug("Inserting {} attrs records in batches of {}...", attrsList.size(), ATTRS_BATCH_SIZE);
        long insertStart = System.currentTimeMillis();

        for (int i = 0; i < attrsList.size(); i += ATTRS_BATCH_SIZE) {
            int end = Math.min(i + ATTRS_BATCH_SIZE, attrsList.size());
            attrsBaseMapper.insertBatch(attrsList.subList(i, end));
        }

        log.debug("Attrs inserted in {}ms", System.currentTimeMillis() - insertStart);
    }

}
