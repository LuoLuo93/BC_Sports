package com.bcsport.admin.task.ihr;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.dto.ihr.*;
import com.bcsport.admin.entity.ihr.*;
import com.bcsport.admin.ihrmapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IHR任务：员工数据同步 */
@Slf4j
@Component("ihrEmployeeTask")
public class IhrEmployeeTask {

    @Autowired
    private IhrApiClient apiClient;

    @Autowired
    private IhrEmployeesAuxiliaryMapper auxiliaryMapper;

    @Autowired
    private IhrEmployeeMapper employeeMapper;

    @Autowired
    private IhrEmployeeDetailMapper detailMapper;

    @Autowired
    private IhrEmployeeFlexAttrMapper flexAttrMapper;

    @Autowired
    private IhrEmployeeSubset04Mapper subset04Mapper;

    @Autowired
    private IhrEmployeeAdditionMapper additionMapper;

    @Autowired
    private IhrEmployeeModificationMapper modificationMapper;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @Autowired
    @Qualifier("ihrTransactionManager")
    private PlatformTransactionManager transactionManager;

    private static final int BATCH_SIZE = 10;  // 减少批量大小，避免超过 SQL Server 2100 参数限制
    private static final int EMPLOYEE_BATCH_SIZE = 100;
    private static final int DETAIL_SUBMIT_BATCH_SIZE = 500;

    // 同步状态跟踪
    private static volatile boolean isSyncing = false;
    private static Date syncStartTime = null;

    /**
     * 同步员工ID列表
     */
    public void syncIds() {
        log.info("=== 开始执行: IHR同步员工ID ===");
        try {
            // API调用在事务外执行
            List<IhrEmployeesAuxiliary> allIds = new ArrayList<>();
            fetchEmployeeIds("", 1, allIds);

            // 短事务仅用于DB写入
            doSyncIds(allIds);

            log.info("=== 完成: IHR同步员工ID, 共{}条 ===", allIds.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步员工ID: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSyncIds(List<IhrEmployeesAuxiliary> allIds) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            auxiliaryMapper.deleteAll();
            for (int i = 0; i < allIds.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allIds.size());
                auxiliaryMapper.insertBatch(allIds.subList(i, end));
            }
            return null;
        });
    }

    private void fetchEmployeeIds(String staffStatus, int pageNo, List<IhrEmployeesAuxiliary> allIds) {
        int currentPage = pageNo;
        while (true) {
            String path = "/openapi/thirdparty/api/staff/v1/staffs/ids?staffstatus=" + staffStatus + "&pageNo=" + currentPage;
            JSONObject response = apiClient.getJsonObject(path);
            JSONObject data = response.getJSONObject("data");
            JSONObject pageInfo = data.getJSONObject("pageInfo");
            int totalPages = pageInfo.getInt("totalPages");
            JSONArray dataList = data.getJSONArray("dataList");

            if (dataList != null) {
                for (int i = 0; i < dataList.size(); i++) {
                    IhrEmployeesAuxiliary entity = new IhrEmployeesAuxiliary();
                    entity.setEmployeesId(dataList.getStr(i));
                    entity.setCreateTime(new Date());
                    allIds.add(entity);
                }
            }

            if (currentPage >= totalPages) {
                break;
            }
            currentPage++;
        }
    }

    /**
     * 同步员工基本信息
     */
    public void syncBasic() {
        log.info("=== 开始执行: IHR同步员工基本信息 ===");
        try {
            // DB读取和API调用在事务外执行
            List<IhrEmployeesAuxiliary> auxiliaries = auxiliaryMapper.selectList(null);
            List<String> staffIds = new ArrayList<>();
            for (IhrEmployeesAuxiliary a : auxiliaries) {
                staffIds.add(a.getEmployeesId());
            }

            // 分批获取（每批100个ID）
            List<IhrEmployee> allEmployees = new ArrayList<>();
            for (int i = 0; i < staffIds.size(); i += EMPLOYEE_BATCH_SIZE) {
                int end = Math.min(i + EMPLOYEE_BATCH_SIZE, staffIds.size());
                List<String> batch = staffIds.subList(i, end);
                allEmployees.addAll(fetchBasicInfoList(batch));
            }

            // 短事务仅用于DB写入
            doSyncBasic(allEmployees);

            log.info("=== 完成: IHR同步员工基本信息, 共{}人 ===", staffIds.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步员工基本信息: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSyncBasic(List<IhrEmployee> allEmployees) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            employeeMapper.deleteAll();
            for (int i = 0; i < allEmployees.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allEmployees.size());
                employeeMapper.insertBatch(allEmployees.subList(i, end));
            }
            return null;
        });
    }

    private List<IhrEmployee> fetchBasicInfoList(List<String> staffIdList) {
        List<IhrEmployee> result = new ArrayList<>();
        String body = JSONUtil.toJsonStr(staffIdList);
        JSONObject response = apiClient.postJsonObject("/openapi/thirdparty/api/staff/v1/staffs/basic", body);
        JSONArray data = response.getJSONArray("data");

        if (data == null || data.isEmpty()) return result;

        for (int i = 0; i < data.size(); i++) {
            JSONObject obj = data.getJSONObject(i);
            IhrEmployeeBasicDTO dto = JSONUtil.toBean(obj, IhrEmployeeBasicDTO.class);
            IhrEmployee e = IhrEntityConverter.toBasicEntity(dto);
            result.add(e);
        }
        return result;
    }

    private String truncate(String value, int maxLength, String fieldName) {
        if (value == null) return null;
        if (value.length() > maxLength) {
            log.warn("字段 {} 超长: 实际长度={}, 最大允许={}, 值前50字符={}",
                    fieldName, value.length(), maxLength, value.substring(0, Math.min(50, value.length())));
            return value.substring(0, maxLength);
        }
        return value;
    }

    /**
     * 同步员工详细信息（多线程，边取边写）
     * 每个线程独立完成 API调用 → 立即入库 → 释放内存，避免数据堆积OOM
     */
    public void syncDetail() {
        log.info("=== 开始执行: IHR同步员工详细信息 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 获取所有员工ID
            List<IhrEmployeesAuxiliary> auxiliaries = auxiliaryMapper.selectList(null);
            List<String> staffIds = new ArrayList<>();
            for (IhrEmployeesAuxiliary a : auxiliaries) {
                staffIds.add(a.getEmployeesId());
            }
            log.info("共 {} 名员工需要同步详情", staffIds.size());

            // 1. 先清空旧数据
            detailMapper.deleteAll();
            flexAttrMapper.deleteAll();

            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            // 2. 多线程并发：每个线程独立完成 拉取→写库→释放
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            int concurrent = 5;
            Semaphore semaphore = new Semaphore(concurrent);

            for (int i = 0; i < staffIds.size(); i += DETAIL_SUBMIT_BATCH_SIZE) {
                int end = Math.min(i + DETAIL_SUBMIT_BATCH_SIZE, staffIds.size());
                List<String> batch = staffIds.subList(i, end);
                CountDownLatch latch = new CountDownLatch(batch.size());
                List<Future<?>> futures = new ArrayList<>(batch.size());

                for (String staffId : batch) {
                    Future<?> future = taskThreadPool.submit(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                // 拉取API
                                List<IhrEmployeeDetail> tempDetailList = new ArrayList<>();
                                List<IhrEmployeeFlexAttr> tempFlexList = new ArrayList<>();
                                fetchEmployeeDetail(staffId, tempDetailList, tempFlexList);

                                // 立即写库（独立短事务）
                                txTemplate.execute(status -> {
                                    if (!tempDetailList.isEmpty()) {
                                        detailMapper.insertBatch(tempDetailList);
                                    }
                                    if (!tempFlexList.isEmpty()) {
                                        flexAttrMapper.insertBatch(tempFlexList);
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
                        } finally {
                            latch.countDown();
                        }
                    });
                    futures.add(future);
                }

                try {
                    boolean completed = latch.await(30, TimeUnit.MINUTES);
                    if (!completed) {
                        futures.forEach(future -> future.cancel(true));
                        log.warn("等待超时，部分任务可能未完成，当前批次: {}-{}", i + 1, end);
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    futures.forEach(future -> future.cancel(true));
                    throw e;
                }
            }

            long fetchTime = System.currentTimeMillis() - startTime;
            log.info("员工详情拉取完成, 成功: {}, 失败: {}, 耗时: {} ms",
                    successCount.get(), failCount.get(), fetchTime);

            // 3. 批量获取并写入自定义子集（批量API，逐批写入）
            // log.info("开始同步子集数据...");
            // syncSubset04InBatches(staffIds, txTemplate);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== 完成: IHR同步员工详细信息, 总耗时: {} ms ===", totalTime);
        } catch (Exception e) {
            log.error("=== 失败: IHR同步员工详细信息: {} ===", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 分批获取子集数据并写入，避免一次性加载到内存
     */
    private void syncSubset04InBatches(List<String> staffIds, TransactionTemplate txTemplate) {
        for (int i = 0; i < staffIds.size(); i += EMPLOYEE_BATCH_SIZE) {
            int end = Math.min(i + EMPLOYEE_BATCH_SIZE, staffIds.size());
            List<String> batch = staffIds.subList(i, end);

            try {
                List<IhrEmployeeSubset04> subsetList = fetchSubset04Batch(batch);
                if (!subsetList.isEmpty()) {
                    txTemplate.execute(status -> {
                        for (int j = 0; j < subsetList.size(); j += BATCH_SIZE) {
                            int subEnd = Math.min(j + BATCH_SIZE, subsetList.size());
                            subset04Mapper.insertBatch(subsetList.subList(j, subEnd));
                        }
                        return null;
                    });
                }
            } catch (Exception e) {
                log.error("子集数据批次写入失败, staffId范围 {}-{}: {}", i + 1, end, e.getMessage());
            }
        }
    }

    /**
     * 获取一批员工的子集数据（每批100个ID）
     */
    private List<IhrEmployeeSubset04> fetchSubset04Batch(List<String> staffIdBatch) {
        List<IhrEmployeeSubset04> result = new ArrayList<>();
        String body = JSONUtil.toJsonStr(staffIdBatch);

        JSONObject response = apiClient.postJsonObject(
                "/openapi/thirdparty/api/staff/v1/subset?metaCode=tab_staff_subset04", body);
        JSONArray data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) return result;

        Map<String, IhrEmployeeSubset04DTO> staffIdMap = new HashMap<>();
        for (int j = 0; j < data.size(); j++) {
            JSONObject obj = data.getJSONObject(j);
            IhrEmployeeSubset04DTO dto = JSONUtil.toBean(obj, IhrEmployeeSubset04DTO.class);
            String staffId = dto.getStaffId();

            if (!staffIdMap.containsKey(staffId)) {
                staffIdMap.put(staffId, dto);
            }
        }

        for (IhrEmployeeSubset04DTO dto : staffIdMap.values()) {
            result.add(IhrEntityConverter.toSubset04Entity(dto));
        }
        return result;
    }

    private void fetchEmployeeDetail(String staffId, List<IhrEmployeeDetail> detailList, List<IhrEmployeeFlexAttr> flexList) {
        String path = "/openapi/thirdparty/api/staff/v1/staffs/" + staffId + "/detail";
        JSONObject response = apiClient.getJsonObject(path);
        JSONObject obj = response.getJSONObject("data");
        if (obj == null) return;

        // 使用 DTO 和 Converter 简化转换
        IhrEmployeeDetailDTO dto = JSONUtil.toBean(obj, IhrEmployeeDetailDTO.class);
        IhrEmployeeDetail detail = IhrEntityConverter.toDetailEntity(dto);
        detailList.add(detail);

        if (dto.getFlexAttributes() != null) {
            IhrEmployeeFlexAttr flexAttr = IhrEntityConverter.toFlexAttrEntity(dto.getId(), dto.getFlexAttributes());
            flexList.add(flexAttr);
        }
    }

    /**
     * 同步今日和昨日新增员工ID
     */
    public void syncAdditions() {
        log.info("=== 开始执行: IHR同步今日和昨日新增员工ID ===");
        try {
            // 获取今天和昨天的日期（时间部分清零）
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();

            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = cal.getTime();

            List<IhrEmployeeAddition> allAdditions = new ArrayList<>();

            // API调用在事务外执行
            log.info("正在获取昨日({})新增员工...", formatDate(yesterday));
            List<IhrEmployeeAddition> yesterdayAdditions = fetchAdditions(yesterday);
            allAdditions.addAll(yesterdayAdditions);

            log.info("正在获取今日({})新增员工...", formatDate(today));
            List<IhrEmployeeAddition> todayAdditions = fetchAdditions(today);
            allAdditions.addAll(todayAdditions);

            // 短事务仅用于DB写入
            doSyncAdditions(yesterday, today, allAdditions);

            log.info("=== 完成: IHR同步新增员工ID, 昨日{}条, 今日{}条, 共{}条 ===",
                    yesterdayAdditions.size(), todayAdditions.size(), allAdditions.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步新增员工ID: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSyncAdditions(Date yesterday, Date today, List<IhrEmployeeAddition> allAdditions) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            additionMapper.deleteBySyncDate(yesterday);
            additionMapper.deleteBySyncDate(today);

            if (!allAdditions.isEmpty()) {
                for (int i = 0; i < allAdditions.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allAdditions.size());
                    additionMapper.insertBatch(allAdditions.subList(i, end));
                }
            }
            return null;
        });
    }

    private String formatDate(Date date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private List<IhrEmployeeAddition> fetchAdditions(Date syncDate) {
        List<IhrEmployeeAddition> result = new ArrayList<>();
        // 确保日期的时间部分清零
        Calendar cal = Calendar.getInstance();
        cal.setTime(syncDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date pureDate = cal.getTime();

        // 格式化日期为 yyyy-MM-dd
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(pureDate);
        int currentPage = 1;

        while (true) {
            String path = "/openapi/thirdparty/api/staff/v1/staffs/additions?date=" + dateStr + "&pageNo=" + currentPage;
            JSONObject response = apiClient.getJsonObject(path);

            // 检查响应状态
            Integer code = response.getInt("code");
            if (code == null || code != 0) {
                log.warn("API返回非成功状态: code=" + code + ", message=" + response.getStr("message"));
                break;
            }

            JSONObject dataObj = response.getJSONObject("data");
            if (dataObj == null) {
                break;
            }

            JSONArray dataList = dataObj.getJSONArray("dataList");
            if (dataList == null || dataList.isEmpty()) {
                break;
            }

            for (int i = 0; i < dataList.size(); i++) {
                String employeesId = dataList.getStr(i);
                IhrEmployeeAddition addition = new IhrEmployeeAddition();
                addition.setEmployeesId(employeesId);
                addition.setSyncDate(pureDate);
                result.add(addition);
            }

            // 获取分页信息，判断是否还有下一页
            JSONObject pageInfo = dataObj.getJSONObject("pageInfo");
            if (pageInfo == null) {
                break;
            }

            Integer totalPages = pageInfo.getInt("totalPages");
            if (totalPages == null || currentPage >= totalPages) {
                break;
            }

            currentPage++;
        }

        return result;
    }

    /**
     * 同步今日和昨日调整员工ID
     */
    public void syncAdjustments() {
        log.info("=== 开始执行: IHR同步今日和昨日调整员工ID ===");
        try {
            // 获取今天和昨天的日期（时间部分清零）
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();

            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = cal.getTime();

            List<IhrEmployeeModification> allModifications = new ArrayList<>();

            // API调用在事务外执行
            log.info("正在获取昨日({})调整员工...", formatDate(yesterday));
            List<IhrEmployeeModification> yesterdayModifications = fetchAdjustments(yesterday);
            allModifications.addAll(yesterdayModifications);

            log.info("正在获取今日({})调整员工...", formatDate(today));
            List<IhrEmployeeModification> todayModifications = fetchAdjustments(today);
            allModifications.addAll(todayModifications);

            // 短事务仅用于DB写入
            doSyncAdjustments(yesterday, today, allModifications);

            log.info("=== 完成: IHR同步调整员工ID, 昨日{}条, 今日{}条, 共{}条 ===",
                    yesterdayModifications.size(), todayModifications.size(), allModifications.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步调整员工ID: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSyncAdjustments(Date yesterday, Date today, List<IhrEmployeeModification> allModifications) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            modificationMapper.deleteBySyncDate(yesterday);
            modificationMapper.deleteBySyncDate(today);

            if (!allModifications.isEmpty()) {
                for (int i = 0; i < allModifications.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allModifications.size());
                    modificationMapper.insertBatch(allModifications.subList(i, end));
                }
            }
            return null;
        });
    }

    private List<IhrEmployeeModification> fetchAdjustments(Date syncDate) {
        List<IhrEmployeeModification> result = new ArrayList<>();
        // 确保日期的时间部分清零
        Calendar cal = Calendar.getInstance();
        cal.setTime(syncDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date pureDate = cal.getTime();

        // 格式化日期为 yyyy-MM-dd
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(pureDate);
        int currentPage = 1;

        while (true) {
            String path = "/openapi/thirdparty/api/staff/v2/staffs/modifications?date=" + dateStr + "&pageNo=" + currentPage + "&staffStatus=IN_SERVICE";
            JSONObject response = apiClient.getJsonObject(path);

            // 检查响应状态
            Integer code = response.getInt("code");
            if (code == null || code != 0) {
                log.warn("API返回非成功状态: code=" + code + ", message=" + response.getStr("message"));
                break;
            }

            JSONObject dataObj = response.getJSONObject("data");
            if (dataObj == null) {
                break;
            }

            JSONArray dataList = dataObj.getJSONArray("dataList");
            if (dataList == null || dataList.isEmpty()) {
                break;
            }

            for (int i = 0; i < dataList.size(); i++) {
                JSONObject obj = dataList.getJSONObject(i);
                String staffId = obj.getStr("staffId");
                Long lastUpdateTs = obj.getLong("lastUpdate");

                IhrEmployeeModification modification = new IhrEmployeeModification();
                modification.setStaffId(staffId);
                if (lastUpdateTs != null) {
                    modification.setLastUpdate(new Date(lastUpdateTs));
                }
                modification.setSyncDate(pureDate);
                result.add(modification);
            }

            // 获取分页信息，判断是否还有下一页
            JSONObject pageInfo = dataObj.getJSONObject("pageInfo");
            if (pageInfo == null) {
                break;
            }

            Integer totalPages = pageInfo.getInt("totalPages");
            if (totalPages == null || currentPage >= totalPages) {
                break;
            }

            currentPage++;
        }

        return result;
    }

    /**
     * 同步全部员工数据（按顺序）
     */
    public void syncAll() {
        // 检查并设置同步状态（双重检查，防止重复调用）
        synchronized (IhrEmployeeTask.class) {
            if (isSyncing) {
                log.warn("同步正在进行中，请勿重复操作");
                return;
            }
            isSyncing = true;
            syncStartTime = new Date();
        }

        log.info("=== 开始执行: IHR同步全部员工数据 ===");
        try {
            // 1. 同步员工ID
            syncIds();

            // 2. 同步新增员工ID
            syncAdditions();

            // 3. 同步调整员工ID
            syncAdjustments();

            // 4. 同步员工基本信息
            syncBasic();

            // 5. 同步员工详情
            syncDetail();

            log.info("=== 完成: IHR同步全部员工数据 ===");
        } catch (Exception e) {
            log.error("=== 失败: IHR同步全部员工数据: {} ===", e.getMessage());
            throw e;
        } finally {
            // 清除同步状态
            synchronized (IhrEmployeeTask.class) {
                isSyncing = false;
                syncStartTime = null;
            }
        }
    }

    /**
     * 获取同步状态
     */
    public static boolean isSyncing() {
        return isSyncing;
    }

    /**
     * 获取同步开始时间
     */
    public static Date getSyncStartTime() {
        return syncStartTime;
    }

    public static void setSyncing(boolean syncing) {
        isSyncing = syncing;
    }

    public static void setSyncStartTime(Date startTime) {
        syncStartTime = startTime;
    }

    /**
     * 手动触发专用：跳过内部状态设置（Controller已设置），直接执行同步步骤
     */
    public void syncAllFromManual() {
        log.info("=== 开始执行: IHR同步全部员工数据(手动触发) ===");
        try {
            syncIds();
            syncAdditions();
            syncAdjustments();
            syncBasic();
            syncDetail();
            log.info("=== 完成: IHR同步全部员工数据 ===");
        } catch (Exception e) {
            log.error("=== 失败: IHR同步全部员工数据: {} ===", e.getMessage());
        } finally {
            synchronized (IhrEmployeeTask.class) {
                isSyncing = false;
                syncStartTime = null;
            }
        }
    }
}
