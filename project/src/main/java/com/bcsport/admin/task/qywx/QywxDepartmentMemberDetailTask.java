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
import org.springframework.transaction.annotation.Transactional;

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
    private static final int PROGRESS_REPORT_INTERVAL = 20; // 每处理20个报告一次进度
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

    /**
     * 用户详情处理结果
     */
    private static class UserDetailResult {
        String userid;
        QywxDepartmentMemberDetail detail;
        List<QywxCustomerListDepartment> deptList;
        boolean success;
        String errorMsg;

        UserDetailResult(String userid, boolean success, String errorMsg) {
            this.userid = userid;
            this.success = success;
            this.errorMsg = errorMsg;
        }

        UserDetailResult(String userid, QywxDepartmentMemberDetail detail, List<QywxCustomerListDepartment> deptList) {
            this.userid = userid;
            this.detail = detail;
            this.deptList = deptList;
            this.success = true;
        }
    }

    // ========================================================================
    // 公开方法：支持多种同步模式
    // ========================================================================

    /**
     * 【推荐】直接从API获取所有部门成员，再获取详情
     * 无需前置任务，一键完成！
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void syncFromApi() {
        log.info("=== Starting: QYWX sync department member detail [MODE: API Direct] ===");
        long startTime = System.currentTimeMillis();

        try {
            // 直接从API获取部门成员列表
            log.info("Fetching department member list from API...");
            List<QywxDepartmentMember> memberList = apiClient.getDepartmentListAll();

            if (memberList == null || memberList.isEmpty()) {
                log.error("=== Completed: No members found from API ===");
                return;
            }

            // 提取userid
            List<String> userIds = new ArrayList<>();
            for (QywxDepartmentMember member : memberList) {
                if (member.getUserid() != null) {
                    userIds.add(member.getUserid());
                }
            }

            log.info("Found {} user ids from API", userIds.size());

            // 执行同步
            doSync(userIds, "API Direct", startTime);
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync department member detail [MODE: API Direct] ===", e);
            throw e;
        }
    }

    /**
     * 从本地FollowUser表获取userid进行同步
     * 需要先执行：QW-同步客户联系成员
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void syncFromFollowUser() {
        log.info("=== Starting: QYWX sync department member detail [MODE: FollowUser] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<String> userIds = followUserMapper.selectAllUserIds();

            if (userIds == null || userIds.isEmpty()) {
                log.error("=== Completed: No follow users found! ===");
                log.info("Please run 'QW-同步客户联系成员' task first!");
                return;
            }

            doSync(userIds, "FollowUser", startTime);
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync department member detail [MODE: FollowUser] ===", e);
            throw e;
        }
    }

    /**
     * 从本地DepartmentMember表获取userid进行同步
     * 需要先执行：QW-同步部门成员
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void syncFromDepartment() {
        log.info("=== Starting: QYWX sync department member detail [MODE: DepartmentMember] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<String> userIds = departmentMemberMapper.selectAllUserIds();

            if (userIds == null || userIds.isEmpty()) {
                log.error("=== Completed: No department members found! ===");
                log.info("Please run 'QW-同步部门成员' task first!");
                return;
            }

            doSync(userIds, "DepartmentMember", startTime);
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync department member detail [MODE: DepartmentMember] ===", e);
            throw e;
        }
    }

    /**
     * 默认模式：按优先级尝试获取userid
     * FollowUser → DepartmentMember → API
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "qywxTransactionManager")
    public void sync() {
        log.info("=== Starting: QYWX sync department member detail [MODE: Auto] ===");
        long startTime = System.currentTimeMillis();

        try {
            List<String> userIds = null;
            String source = null;

            // 1. 尝试从FollowUser获取
            userIds = followUserMapper.selectAllUserIds();
            source = "FollowUser";

            // 2. 如果没有，尝试从DepartmentMember获取
            if (userIds == null || userIds.isEmpty()) {
                log.warn("No follow users found, trying DepartmentMember...");
                userIds = departmentMemberMapper.selectAllUserIds();
                source = "DepartmentMember";
            }

            // 3. 如果还没有，直接从API获取
            if (userIds == null || userIds.isEmpty()) {
                log.warn("No department members found, trying API directly...");
                log.info("Fetching department member list from API...");
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
                log.error("=== Completed: NO USER IDS FOUND FROM ANY SOURCE ===");
                return;
            }

            doSync(userIds, source, startTime);
        } catch (Exception e) {
            log.error("=== Failed: QYWX sync department member detail [MODE: Auto] ===", e);
            throw e;
        }
    }

    // ========================================================================
    // 私有方法：核心同步逻辑
    // ========================================================================

    private void doSync(List<String> userIds, String source, long startTime) {
        log.info("Found {} user ids from {}", userIds.size(), source);

        // 清空表
        detailMapper.deleteAll();
        departmentMapper.deleteAll();
        log.info("Cleared old data from tables");

        // 并发获取用户详情
        List<UserDetailResult> results = fetchUserDetailsConcurrently(userIds);

        // 收集结果
        List<QywxDepartmentMemberDetail> detailList = new ArrayList<>();
        List<QywxCustomerListDepartment> deptList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (UserDetailResult result : results) {
            if (result.success && result.detail != null) {
                detailList.add(result.detail);
                if (result.deptList != null) {
                    deptList.addAll(result.deptList);
                }
                successCount++;
            } else {
                failCount++;
                log.debug("Failed to fetch userid: {}, reason: {}", result.userid, result.errorMsg);
            }
        }

        log.info("Fetched {} user details successfully, {} failed", successCount, failCount);

        // 批量插入成员详情
        if (!detailList.isEmpty()) {
            log.info("Inserting {} department member details...", detailList.size());
            long insertStart = System.currentTimeMillis();
            for (int i = 0; i < detailList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, detailList.size());
                detailMapper.insertBatch(detailList.subList(i, end));
                if ((i + BATCH_SIZE) % (BATCH_SIZE * 5) == 0 || end == detailList.size()) {
                    log.info("Inserted {}/{} details", Math.min(end, detailList.size()), detailList.size());
                }
            }
            log.info("Details inserted in {}ms", System.currentTimeMillis() - insertStart);
        }

        // 批量插入部门关系
        if (!deptList.isEmpty()) {
            log.info("Inserting {} department relationships...", deptList.size());
            long insertStart = System.currentTimeMillis();
            for (int i = 0; i < deptList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, deptList.size());
                departmentMapper.insertBatch(deptList.subList(i, end));
                if ((i + BATCH_SIZE) % (BATCH_SIZE * 5) == 0 || end == deptList.size()) {
                    log.info("Inserted {}/{} relationships", Math.min(end, deptList.size()), deptList.size());
                }
            }
            log.info("Relationships inserted in {}ms", System.currentTimeMillis() - insertStart);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== Completed: QYWX sync department member detail [MODE: {}], details: {}, departments: {}, total time: {}ms ===",
                source, detailList.size(), deptList.size(), totalTime);
    }

    /**
     * 并发获取用户详情（使用统一线程池）
     */
    private List<UserDetailResult> fetchUserDetailsConcurrently(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Starting to fetch {} user details using shared thread pool...", userIds.size());
        long fetchStart = System.currentTimeMillis();

        AtomicInteger progressCounter = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(userIds.size());
        List<UserDetailResult> results = Collections.synchronizedList(new ArrayList<>());

        try {
            // 提交任务到统一线程池
            for (String userid : userIds) {
                taskThreadPool.submit(() -> {
                    try {
                        UserDetailResult result = fetchSingleUserDetail(userid, progressCounter, userIds.size());
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
            log.info("Fetched {} user details in {}ms (avg: {}ms/user)",
                    userIds.size(), fetchTime, userIds.size() > 0 ? fetchTime / userIds.size() : 0);

            return results;
        } catch (Exception e) {
            log.error("Error fetching user details", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取单个用户详情
     */
    private UserDetailResult fetchSingleUserDetail(String userid, AtomicInteger progressCounter, int total) {
        try {
            JSONObject userDetail = apiClient.getUserDetail(userid);
            if (userDetail == null) {
                return new UserDetailResult(userid, false, "User detail is null");
            }

            // 检查返回是否有错误
            Integer errcode = userDetail.getInt("errcode");
            if (errcode != null && errcode != 0) {
                log.warn("API returned error for userid: {}, errcode: {}, errmsg: {}",
                        userid, errcode, userDetail.getStr("errmsg"));
                return new UserDetailResult(userid, false,
                        "errcode: " + errcode + ", errmsg: " + userDetail.getStr("errmsg"));
            }

            // 检查返回中是否有userid（成功的标志）
            if (!userDetail.containsKey("userid")) {
                log.warn("API returned success but no userid for userid: {}", userid);
                return new UserDetailResult(userid, false, "Missing userid in response");
            }

            // 构建成员详情对象
            QywxDepartmentMemberDetail detail = new QywxDepartmentMemberDetail();
            detail.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
            detail.setUserid(userDetail.getStr("userid", userid));
            detail.setName(userDetail.getStr("name", ""));
            detail.setOpenUserid(userDetail.getStr("open_userid", ""));
            detail.setMainDepartment(userDetail.getStr("main_department", ""));
            detail.setPosition(userDetail.getStr("position", ""));
            detail.setMobile(userDetail.getStr("mobile", ""));
            detail.setStatus(userDetail.getStr("status", ""));

            // 构建部门关系对象
            List<QywxCustomerListDepartment> userDeptList = new ArrayList<>();
            JSONArray departments = userDetail.getJSONArray("department");
            if (departments != null && departments.size() > 0) {
                for (int i = 0; i < departments.size(); i++) {
                    QywxCustomerListDepartment dept = new QywxCustomerListDepartment();
                    dept.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                    dept.setUserid(detail.getUserid());
                    dept.setDepartment(departments.getInt(i));
                    userDeptList.add(dept);
                }
            }

            // 报告进度
            int current = progressCounter.incrementAndGet();
            if (current % PROGRESS_REPORT_INTERVAL == 0 || current == total) {
                log.info("Progress: {}/{} ({}%)", current, total, (current * 100 / total));
            }

            return new UserDetailResult(userid, detail, userDeptList);
        } catch (Exception e) {
            log.warn("Exception for userid: {}", userid, e);
            return new UserDetailResult(userid, false, e.getMessage());
        }
    }
}
