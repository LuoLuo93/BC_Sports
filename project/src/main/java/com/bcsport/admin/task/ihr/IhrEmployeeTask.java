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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int BATCH_SIZE = 10;  // 减少批量大小，避免超过 SQL Server 2100 参数限制
    private static final int EMPLOYEE_BATCH_SIZE = 100;

    /**
     * 同步员工ID列表
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "ihrTransactionManager")
    public void syncIds() {
        log.info("=== 开始执行: IHR同步员工ID ===");
        try {
            // 先获取所有数据到内存
            List<IhrEmployeesAuxiliary> allIds = new ArrayList<>();
            fetchEmployeeIds("", 1, allIds);

            // 再在事务中删除并插入
            auxiliaryMapper.deleteAll();
            // 分批插入
            for (int i = 0; i < allIds.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allIds.size());
                auxiliaryMapper.insertBatch(allIds.subList(i, end));
            }
            log.info("=== 完成: IHR同步员工ID, 共{}条 ===", allIds.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步员工ID: {} ===", e.getMessage());
            throw e;
        }
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
    @Transactional(rollbackFor = Exception.class, transactionManager = "ihrTransactionManager")
    public void syncBasic() {
        log.info("=== 开始执行: IHR同步员工基本信息 ===");
        try {
            // 先获取所有数据到内存
            // 获取所有员工ID
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

            // 再在事务中删除并插入
            employeeMapper.deleteAll();
            // 分批插入
            for (int i = 0; i < allEmployees.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allEmployees.size());
                employeeMapper.insertBatch(allEmployees.subList(i, end));
            }
            log.info("=== 完成: IHR同步员工基本信息, 共{}人 ===", staffIds.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步员工基本信息: {} ===", e.getMessage());
            throw e;
        }
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
     * 同步员工详细信息
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "ihrTransactionManager")
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

            // 多线程并发获取员工详情
            List<IhrEmployeeDetail> allDetailList = Collections.synchronizedList(new ArrayList<>());
            List<IhrEmployeeFlexAttr> allFlexList = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 创建线程池，10个并发线程
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<Future<?>> futures = new ArrayList<>();

            for (String staffId : staffIds) {
                Future<?> future = executorService.submit(() -> {
                    try {
                        List<IhrEmployeeDetail> tempDetailList = new ArrayList<>();
                        List<IhrEmployeeFlexAttr> tempFlexList = new ArrayList<>();
                        fetchEmployeeDetail(staffId, tempDetailList, tempFlexList);
                        allDetailList.addAll(tempDetailList);
                        allFlexList.addAll(tempFlexList);
                        successCount.incrementAndGet();
                        if (successCount.get() % 100 == 0) {
                            log.info("已处理 {}/{} 名员工", successCount.get(), staffIds.size());
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        log.warn("获取员工详情失败, staffId: {}, 错误: {}", staffId, e.getMessage());
                    }
                });
                futures.add(future);
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("等待任务完成时出错", e);
                }
            }
            executorService.shutdown();

            // 批量获取自定义子集（这个是批量接口，不需要多线程）
            List<IhrEmployeeSubset04> allSubset04List = fetchSubset04List(staffIds);

            long endTime = System.currentTimeMillis();
            log.info("数据获取完成，耗时 {} ms，成功 {} 个，失败 {} 个",
                    (endTime - startTime), successCount.get(), failCount.get());

            // 再在事务中删除并插入
            log.info("开始写入数据库...");
            detailMapper.deleteAll();
            flexAttrMapper.deleteAll();
            subset04Mapper.deleteAll();

            // 分批插入
            for (int i = 0; i < allDetailList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allDetailList.size());
                detailMapper.insertBatch(allDetailList.subList(i, end));
            }
            for (int i = 0; i < allFlexList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allFlexList.size());
                flexAttrMapper.insertBatch(allFlexList.subList(i, end));
            }
            for (int i = 0; i < allSubset04List.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allSubset04List.size());
                subset04Mapper.insertBatch(allSubset04List.subList(i, end));
            }

            log.info("=== 完成: IHR同步员工详细信息, 共{}人 ===", successCount.get());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步员工详细信息: {} ===", e.getMessage());
            throw e;
        }
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

    private List<IhrEmployeeSubset04> fetchSubset04List(List<String> staffIds) {
        List<IhrEmployeeSubset04> result = new ArrayList<>();
        Map<String, List<IhrEmployeeSubset04DTO>> staffIdDataMap = new HashMap<>();

        // 每批100个员工ID
        for (int i = 0; i < staffIds.size(); i += EMPLOYEE_BATCH_SIZE) {
            int end = Math.min(i + EMPLOYEE_BATCH_SIZE, staffIds.size());
            List<String> batch = staffIds.subList(i, end);
            String body = JSONUtil.toJsonStr(batch);

            JSONObject response = apiClient.postJsonObject(
                    "/openapi/thirdparty/api/staff/v1/subset?metaCode=tab_staff_subset04", body);
            JSONArray data = response.getJSONArray("data");
            if (data == null || data.isEmpty()) continue;

            for (int j = 0; j < data.size(); j++) {
                JSONObject obj = data.getJSONObject(j);
                IhrEmployeeSubset04DTO dto = JSONUtil.toBean(obj, IhrEmployeeSubset04DTO.class);
                String staffId = dto.getStaffId();

                // 先收集数据，看看重复情况
                staffIdDataMap.computeIfAbsent(staffId, k -> new ArrayList<>()).add(dto);
            }
        }

        // 检查并打印重复数据
        for (Map.Entry<String, List<IhrEmployeeSubset04DTO>> entry : staffIdDataMap.entrySet()) {
            String staffId = entry.getKey();
            List<IhrEmployeeSubset04DTO> list = entry.getValue();

            if (list.size() > 1) {
                log.warn("staffId {} 有 {} 条数据:", staffId, list.size());
                for (int k = 0; k < list.size(); k++) {
                    log.warn("  第 {} 条: {}", k + 1, JSONUtil.toJsonStr(list.get(k)));
                }
            }

            // 只取第一条并转换为实体
            IhrEmployeeSubset04 entity = IhrEntityConverter.toSubset04Entity(list.get(0));
            result.add(entity);
        }

        return result;
    }

    /**
     * 同步今日和昨日新增员工ID
     */
    @Transactional(rollbackFor = Exception.class, transactionManager = "ihrTransactionManager")
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

            // 获取昨天的数据
            log.info("正在获取昨日({})新增员工...", formatDate(yesterday));
            List<IhrEmployeeAddition> yesterdayAdditions = fetchAdditions(yesterday);
            allAdditions.addAll(yesterdayAdditions);

            // 获取今天的数据
            log.info("正在获取今日({})新增员工...", formatDate(today));
            List<IhrEmployeeAddition> todayAdditions = fetchAdditions(today);
            allAdditions.addAll(todayAdditions);

            // 删除这两天已存在的数据
            log.info("正在删除数据库中昨日和今日的旧数据...");
            additionMapper.deleteBySyncDate(yesterday);
            additionMapper.deleteBySyncDate(today);

            // 分批插入新数据
            if (!allAdditions.isEmpty()) {
                log.info("正在插入{}条新增员工数据...", allAdditions.size());
                for (int i = 0; i < allAdditions.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allAdditions.size());
                    additionMapper.insertBatch(allAdditions.subList(i, end));
                }
            }

            log.info("=== 完成: IHR同步新增员工ID, 昨日{}条, 今日{}条, 共{}条 ===",
                    yesterdayAdditions.size(), todayAdditions.size(), allAdditions.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步新增员工ID: {} ===", e.getMessage());
            throw e;
        }
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
    @Transactional(rollbackFor = Exception.class, transactionManager = "ihrTransactionManager")
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

            // 获取昨天的数据
            log.info("正在获取昨日({})调整员工...", formatDate(yesterday));
            List<IhrEmployeeModification> yesterdayModifications = fetchAdjustments(yesterday);
            allModifications.addAll(yesterdayModifications);

            // 获取今天的数据
            log.info("正在获取今日({})调整员工...", formatDate(today));
            List<IhrEmployeeModification> todayModifications = fetchAdjustments(today);
            allModifications.addAll(todayModifications);

            // 删除这两天已存在的数据
            log.info("正在删除数据库中昨日和今日的旧数据...");
            modificationMapper.deleteBySyncDate(yesterday);
            modificationMapper.deleteBySyncDate(today);

            // 分批插入新数据
            if (!allModifications.isEmpty()) {
                log.info("正在插入{}条调整员工数据...", allModifications.size());
                for (int i = 0; i < allModifications.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allModifications.size());
                    modificationMapper.insertBatch(allModifications.subList(i, end));
                }
            }

            log.info("=== 完成: IHR同步调整员工ID, 昨日{}条, 今日{}条, 共{}条 ===",
                    yesterdayModifications.size(), todayModifications.size(), allModifications.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步调整员工ID: {} ===", e.getMessage());
            throw e;
        }
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
        }
    }
}
