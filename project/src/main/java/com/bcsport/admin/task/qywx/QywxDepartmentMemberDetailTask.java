package com.bcsport.admin.task.qywx;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.QywxCustomerListDepartment;
import com.bcsport.admin.entity.qywx.QywxDepartmentMember;
import com.bcsport.admin.entity.qywx.QywxDepartmentMemberDetail;
import com.bcsport.admin.qywxmapper.QywxCustomerListDepartmentMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberDetailMapper;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberMapper;
import com.bcsport.admin.qywxmapper.QywxFollowUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业微信任务：同步部门成员详情（包含职位、手机等信息）
 * 优化版：支持并发获取用户详情，支持多种同步模式
 *
 * 支持的同步模式（通过方法参数选择）：
 * - syncFromFollowUser()    : 从本地FollowUser表获取userid（需要先执行同步客户联系成员）
 * - syncFromDepartment()    : 从本地DepartmentMember表获取userid（需要先执行同步部门成员）
 * - syncFromApi()           : 【推荐】直接从API获取所有部门成员，再获取详情（无需前置任务）
 * - sync()                  : 默认模式，按优先级尝试：FollowUser → DepartmentMember → API
 */
@Slf4j
@Component("qywxDepartmentMemberDetailTask")
public class QywxDepartmentMemberDetailTask {

    private static final int BATCH_SIZE = 50;
    private static final int CONCURRENT_THREADS = 10; // 并发线程数
    private static final long THREAD_POOL_TIMEOUT_SECONDS = 300; // 线程池等待超时时间（5分钟）

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxFollowUserMapper followUserMapper;

    @Autowired
    private QywxDepartmentMemberMapper departmentMemberMapper;

    @Autowired
    private QywxDepartmentMemberDetailMapper detailMapper;

    @Autowired
    private QywxCustomerListDepartmentMapper departmentMapper;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    // ========================================================================
    // 公开方法：支持多种同步模式
    // ========================================================================

    /**
     * 【推荐】直接从API获取所有部门成员，再获取详情
     * 无需前置任务，一键完成！
     */
    public void syncFromApi() {
        log.info("=== 开始执行: 同步部门成员详情 [API直连] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<QywxDepartmentMember> memberList = apiClient.getDepartmentListAll();

            if (memberList == null || memberList.isEmpty()) {
                log.warn("=== 完成: API未返回成员数据 ===");
                return;
            }

            List<String> userIds = new ArrayList<>();
            for (QywxDepartmentMember member : memberList) {
                if (member.getUserid() != null) {
                    userIds.add(member.getUserid());
                }
            }

            log.info("API获取到 {} 个成员", userIds.size());

            // 执行同步
            doSync(userIds, "API Direct", startTime);
        } catch (Exception e) {
            log.error("=== 失败: 同步部门成员详情 [API直连] ===", e);
            throw e;
        }
    }

    /**
     * 从本地FollowUser表获取userid进行同步
     * 需要先执行：QW-同步客户联系成员
     */
    public void syncFromFollowUser() {
        log.info("=== 开始执行: 同步部门成员详情 [FollowUser] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<String> userIds = followUserMapper.selectAllUserIds();

            if (userIds == null || userIds.isEmpty()) {
                log.warn("=== 完成: 无客户联系成员数据, 请先执行'QW-同步客户联系成员' ===");
                return;
            }

            doSync(userIds, "FollowUser", startTime);
        } catch (Exception e) {
            log.error("=== 失败: 同步部门成员详情 [FollowUser] ===", e);
            throw e;
        }
    }

    /**
     * 从本地DepartmentMember表获取userid进行同步
     * 需要先执行：QW-同步部门成员
     */
    public void syncFromDepartment() {
        log.info("=== 开始执行: 同步部门成员详情 [DepartmentMember] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<String> userIds = departmentMemberMapper.selectAllUserIds();

            if (userIds == null || userIds.isEmpty()) {
                log.warn("=== 完成: 无部门成员数据, 请先执行'QW-同步部门成员' ===");
                return;
            }

            doSync(userIds, "DepartmentMember", startTime);
        } catch (Exception e) {
            log.error("=== 失败: 同步部门成员详情 [DepartmentMember] ===", e);
            throw e;
        }
    }

    /**
     * 默认模式：按优先级尝试获取userid
     * FollowUser → DepartmentMember → API
     */
    public void sync() {
        log.info("=== 开始执行: 同步部门成员详情 [自动模式] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<String> userIds = null;
            String source = null;

            // 1. 尝试从FollowUser获取
            userIds = followUserMapper.selectAllUserIds();
            source = "FollowUser";

            // 2. 如果没有，尝试从DepartmentMember获取
            if (userIds == null || userIds.isEmpty()) {
                log.info("无客户联系成员数据, 尝试部门成员...");
                userIds = departmentMemberMapper.selectAllUserIds();
                source = "DepartmentMember";
            }

            // 3. 如果还没有，直接从API获取
            if (userIds == null || userIds.isEmpty()) {
                log.info("无部门成员数据, 尝试API直连...");
                List<QywxDepartmentMember> memberList = apiClient.getDepartmentListAll();
                if (memberList != null && !memberList.isEmpty()) {
                    userIds = new ArrayList<>();
                    for (QywxDepartmentMember member : memberList) {
                        if (member.getUserid() != null) {
                            userIds.add(member.getUserid());
                        }
                    }
                    source = "API Direct";
                }
            }

            // 4. 最终检查
            if (userIds == null || userIds.isEmpty()) {
                log.warn("=== 完成: 无法从任何数据源获取成员 ===");
                return;
            }

            doSync(userIds, source, startTime);
        } catch (Exception e) {
            log.error("=== 失败: 同步部门成员详情 [自动模式] ===", e);
            throw e;
        }
    }

    // ========================================================================
    // 私有方法：核心同步逻辑
    // ========================================================================

    private void doSync(List<String> userIds, String source, long startTime) {
        log.info("从 {} 获取到 {} 个成员", source, userIds.size());

        // 先清空旧数据
        new TransactionTemplate(transactionManager).execute(status -> {
            detailMapper.deleteAll();
            departmentMapper.deleteAll();
            return null;
        });

        // 边拉边写：每个线程独立完成 API→解析→写库→释放
        fetchAndWriteConcurrently(userIds);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== 完成: 同步部门成员详情 [{}], 耗时: {} ms ===", source, totalTime);
    }

    /**
     * 并发拉取并写库：每个线程独立完成 拉取→解析→写库→释放内存
     */
    private void fetchAndWriteConcurrently(List<String> userIds) {
        if (userIds.isEmpty()) return;

        long fetchStart = System.currentTimeMillis();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        int concurrent = CONCURRENT_THREADS;
        Semaphore semaphore = new Semaphore(concurrent);
        CountDownLatch latch = new CountDownLatch(userIds.size());
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        List<Future<?>> futures = new ArrayList<>(userIds.size());

        for (String userid : userIds) {
            Future<?> future = taskThreadPool.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        // 拉取
                        JSONObject userDetail = apiClient.getUserDetail(userid);
                        if (userDetail == null) {
                            failCount.incrementAndGet();
                            return;
                        }

                        Integer errcode = userDetail.getInt("errcode");
                        if (errcode != null && errcode != 0) {
                            failCount.incrementAndGet();
                            return;
                        }
                        if (!userDetail.containsKey("userid")) {
                            failCount.incrementAndGet();
                            return;
                        }

                        // 解析
                        QywxDepartmentMemberDetail detail = new QywxDepartmentMemberDetail();
                        detail.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                        detail.setUserid(userDetail.getStr("userid", userid));
                        detail.setName(userDetail.getStr("name", ""));
                        detail.setOpenUserid(userDetail.getStr("open_userid", ""));
                        detail.setMainDepartment(userDetail.getStr("main_department", ""));
                        detail.setPosition(userDetail.getStr("position", ""));
                        detail.setMobile(userDetail.getStr("mobile", ""));
                        detail.setStatus(userDetail.getStr("status", ""));

                        List<QywxCustomerListDepartment> deptList = new ArrayList<>();
                        JSONArray departments = userDetail.getJSONArray("department");
                        if (departments != null && departments.size() > 0) {
                            for (int i = 0; i < departments.size(); i++) {
                                QywxCustomerListDepartment dept = new QywxCustomerListDepartment();
                                dept.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                                dept.setUserid(detail.getUserid());
                                dept.setDepartment(departments.getInt(i));
                                deptList.add(dept);
                            }
                        }

                        // 立即写库（独立短事务）
                        List<QywxCustomerListDepartment> finalDeptList = deptList;
                        txTemplate.execute(status -> {
                            detailMapper.insertBatch(Collections.singletonList(detail));
                            if (!finalDeptList.isEmpty()) {
                                departmentMapper.insertBatch(finalDeptList);
                            }
                            return null;
                        });

                        successCount.incrementAndGet();
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("Failed to fetch userid: {}, error: {}", userid, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        try {
            boolean completed = latch.await(THREAD_POOL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                futures.forEach(future -> future.cancel(true));
            }
            if (!completed) {
                log.warn("等待超时, 部分任务可能未完成");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            futures.forEach(future -> future.cancel(true));
        }

        long fetchTime = System.currentTimeMillis() - fetchStart;
        log.info("成员详情拉取完成, 成功: {}, 失败: {}, 耗时: {} ms",
                successCount.get(), failCount.get(), fetchTime);
    }

}
