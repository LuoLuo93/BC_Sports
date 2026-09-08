package com.bcsport.admin.task.qywx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bcsport.admin.entity.qywx.VxCustomerBase;
import com.bcsport.admin.entity.qywx.VxCustomerBaseDetails;
import com.bcsport.admin.entity.qywx.VxCustomerBaseDetailsGroupMembers;
import com.bcsport.admin.qywxmapper.QywxDepartmentMemberMapper;
import com.bcsport.admin.qywxmapper.VxCustomerBaseDetailsMapper;
import com.bcsport.admin.qywxmapper.VxCustomerBaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业微信群同步任务
 * 多线程获取API数据，每个线程获取完后直接写库（独立短事务）
 */
@Slf4j
@Component("qywxGroupChatTask")
public class QywxGroupChatTask {

    private static final int BATCH_SIZE = 50;
    private static final int USER_BATCH_SIZE = 100;
    private static final int CONCURRENT_TASKS = 3;
    private static volatile boolean isSyncing = false;

    public static boolean isSyncing() { return isSyncing; }

    @Autowired
    private QywxApiClient apiClient;

    @Autowired
    private QywxDepartmentMemberMapper departmentMemberMapper;

    @Autowired
    private VxCustomerBaseMapper customerBaseMapper;

    @Autowired
    private VxCustomerBaseDetailsMapper customerBaseDetailsMapper;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    public void sync() {
        synchronized (QywxGroupChatTask.class) {
            if (isSyncing) { log.warn("同步企微群聊正在进行中"); return; }
            isSyncing = true;
        }
        log.info("=== 开始执行: 同步企微群聊 ===");
        long totalStartTime = System.currentTimeMillis();

        try {
            // 第一步：获取所有成员的群列表（小表内存缓冲，全部拉完后单事务删旧+插新，
            //         拉取中途失败时群列表主表不受影响）
            Set<String> chatIds = syncGroupChatList();

            // 第二步：获取所有群详情（多线程，边获取边写影子表，成功后原子切换）
            if (!chatIds.isEmpty()) {
                int[] result = syncGroupChatDetails(new ArrayList<>(chatIds));
                int failed = result[1];
                // 失败率超10%(典型如token失效导致全部失败)时放弃切换，保留主表旧数据
                if (failed * 10 > chatIds.size()) {
                    throw new IllegalStateException(String.format(
                            "同步群详情失败率过高(失败 %d/%d)，保留主表旧数据不切换", failed, chatIds.size()));
                }
                new TransactionTemplate(transactionManager).execute(status -> {
                    customerBaseDetailsMapper.deleteAllDetails();
                    customerBaseDetailsMapper.copyDetailsFromStg();
                    customerBaseDetailsMapper.clearStgDetails();
                    customerBaseDetailsMapper.deleteAllGroupMembers();
                    customerBaseDetailsMapper.copyGroupMembersFromStg();
                    customerBaseDetailsMapper.clearStgGroupMembers();
                    return null;
                });
            }

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== 完成: 同步企微群聊, 耗时: {} ms ===", totalTime);

        } catch (Exception e) {
            log.error("=== 失败: 同步企微群聊 ===", e);
            throw new RuntimeException(e);
        } finally {
            synchronized (QywxGroupChatTask.class) { isSyncing = false; }
        }
    }

    /**
     * 第一步：获取所有成员的群列表。群列表每群仅一行(小表)，全量内存缓冲后
     * 在一个事务里完成"删旧+插新"，中途失败不触碰主表。
     */
    private Set<String> syncGroupChatList() {
        long startTime = System.currentTimeMillis();

        List<String> userIds = departmentMemberMapper.selectAllUserIds();
        if (userIds == null || userIds.isEmpty()) {
            log.info("没有成员数据，跳过群列表获取");
            return Collections.emptySet();
        }

        log.info("获取到 {} 个成员，开始批量查询群列表", userIds.size());

        Set<String> allChatIds = new HashSet<>();
        List<VxCustomerBase> allRows = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i += USER_BATCH_SIZE) {
            int end = Math.min(i + USER_BATCH_SIZE, userIds.size());
            List<String> batchUserIds = userIds.subList(i, end);

            String cursor = "";
            do {
                JSONObject result = apiClient.getGroupChatList(batchUserIds, cursor);

                JSONArray groupChatList = result.getJSONArray("group_chat_list");
                if (groupChatList != null && groupChatList.size() > 0) {
                    for (int j = 0; j < groupChatList.size(); j++) {
                        JSONObject item = groupChatList.getJSONObject(j);
                        String chatId = item.getStr("chat_id", "");

                        if (chatId.length() > 0 && !allChatIds.contains(chatId)) {
                            VxCustomerBase groupChat = new VxCustomerBase();
                            groupChat.setChatId(chatId);
                            groupChat.setStatus(item.getStr("status", ""));
                            allRows.add(groupChat);
                            allChatIds.add(chatId);
                        }
                    }
                }

                cursor = result.getStr("next_cursor", "");
            } while (cursor != null && cursor.length() > 0);
        }

        // 全部拉完后单事务切换（分批插入避免单语句参数超限）
        if (!allRows.isEmpty()) {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                customerBaseMapper.deleteAll();
                for (int i = 0; i < allRows.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allRows.size());
                    customerBaseMapper.insertBatch(allRows.subList(i, end));
                }
                return null;
            });
        }

        log.info("群列表获取完成, 共 {} 个群, 耗时: {} ms", allChatIds.size(), System.currentTimeMillis() - startTime);
        return allChatIds;
    }

    /**
     * 第二步：多线程获取群详情，每个线程获取完直接写影子表，成功后由调用方原子切换
     *
     * @return [成功数, 失败数]
     */
    private int[] syncGroupChatDetails(List<String> chatIds) {
        long startTime = System.currentTimeMillis();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 1. 只清空影子表(清掉上轮残留)。主表不动：拉取中途失败时旧数据完整保留
        txTemplate.execute(status -> {
            customerBaseDetailsMapper.clearStgDetails();
            customerBaseDetailsMapper.clearStgGroupMembers();
            return null;
        });

        if (chatIds == null || chatIds.isEmpty()) {
            log.info("没有群数据，跳过");
            return new int[]{0, 0};
        }

        log.info("共 {} 个群，开始并行获取详情", chatIds.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger totalMembers = new AtomicInteger(0);

        Semaphore semaphore = new Semaphore(CONCURRENT_TASKS);
        CountDownLatch totalLatch = new CountDownLatch(chatIds.size());

        for (String chatId : chatIds) {
            taskThreadPool.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        // 1. 获取群详情（API请求）
                        GroupDetailResult result = fetchGroupDetail(chatId);

                        if (result != null && result.success) {
                            // 2. 立即写影子表（独立短事务）
                            txTemplate.execute(status -> {
                                // 写入群详情
                                customerBaseDetailsMapper.insertDetailStg(result.detail);

                                // 写入群成员
                                if (result.members != null && !result.members.isEmpty()) {
                                    for (int i = 0; i < result.members.size(); i += BATCH_SIZE) {
                                        int end = Math.min(i + BATCH_SIZE, result.members.size());
                                        customerBaseDetailsMapper.insertGroupMembersBatchStg(
                                                result.members.subList(i, end));
                                    }
                                }
                                return null;
                            });

                            totalMembers.addAndGet(result.members != null ? result.members.size() : 0);
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("处理群详情失败，chatId: {}", chatId, e);
                    failCount.incrementAndGet();
                } finally {
                    totalLatch.countDown();
                }
            });
        }

        try {
            boolean completed = totalLatch.await(30, TimeUnit.MINUTES);
            if (!completed) {
                // 超时=部分群未执行完，影子表数据不完整，必须放弃切换保住主表旧数据
                throw new IllegalStateException("等待群详情获取超时(30分钟)，本轮放弃切换");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("群详情同步被中断，本轮放弃切换", e);
        }

        log.info("群详情获取完成, 成功: {}, 失败: {}, 总群成员: {}, 耗时: {} ms",
                successCount.get(), failCount.get(), totalMembers.get(),
                System.currentTimeMillis() - startTime);
        return new int[]{successCount.get(), failCount.get()};
    }

    /**
     * 获取单个群详情
     */
    private GroupDetailResult fetchGroupDetail(String chatId) {
        try {
            JSONObject result = apiClient.getGroupChatDetail(chatId);
            Integer errcode = result.getInt("errcode");
            if (errcode != null && errcode != 0) {
                return null;
            }

            JSONObject groupChat = result.getJSONObject("group_chat");
            if (groupChat == null) {
                return null;
            }

            // 解析群信息
            VxCustomerBaseDetails detail = new VxCustomerBaseDetails();
            detail.setChatId(groupChat.getStr("chat_id", ""));
            detail.setName(groupChat.getStr("name", ""));
            detail.setOwner(groupChat.getStr("owner", ""));
            String createTime = groupChat.getStr("create_time", "");
            if (createTime != null && createTime.length() > 0) {
                try {
                    Date date = new Date(Long.parseLong(createTime) * 1000);
                    detail.setCreateTime(DateUtil.formatDateTime(date));
                } catch (Exception e) {
                    detail.setCreateTime(createTime);
                }
            }

            // 解析群成员
            JSONArray memberList = groupChat.getJSONArray("member_list");
            List<VxCustomerBaseDetailsGroupMembers> members = new ArrayList<>();
            if (memberList != null && memberList.size() > 0) {
                for (int i = 0; i < memberList.size(); i++) {
                    JSONObject member = memberList.getJSONObject(i);
                    VxCustomerBaseDetailsGroupMembers m = new VxCustomerBaseDetailsGroupMembers();
                    m.setChatId(detail.getChatId());
                    m.setUserid(member.getStr("userid", ""));
                    m.setType(member.getStr("type", ""));
                    m.setUnionId(member.getStr("unionid", ""));
                    m.setName(member.getStr("name", ""));
                    m.setJoinScene(member.getStr("join_scene", ""));

                    String joinTime = member.getStr("join_time", "");
                    if (joinTime != null && joinTime.length() > 0) {
                        try {
                            Date date = new Date(Long.parseLong(joinTime) * 1000);
                            m.setJoinTime(DateUtil.formatDateTime(date));
                        } catch (Exception e) {
                            m.setJoinTime(joinTime);
                        }
                    }

                    JSONObject invitor = member.getJSONObject("invitor");
                    if (invitor != null) {
                        m.setInvitor(invitor.getStr("userid", ""));
                    }

                    members.add(m);
                }
            }

            detail.setMemberList(members.size());
            return new GroupDetailResult(true, detail, members);

        } catch (Exception e) {
            log.error("获取群详情失败，chatId: {}", chatId, e);
            return null;
        }
    }

    private static class GroupDetailResult {
        boolean success;
        VxCustomerBaseDetails detail;
        List<VxCustomerBaseDetailsGroupMembers> members;

        GroupDetailResult(boolean success, VxCustomerBaseDetails detail, List<VxCustomerBaseDetailsGroupMembers> members) {
            this.success = success;
            this.detail = detail;
            this.members = members;
        }
    }
}
