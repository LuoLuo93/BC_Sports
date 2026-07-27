package com.bcsport.admin.service.impl;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpEmployeeQueryDTO;
import com.bcsport.admin.entity.ihr.ErpEmployeeSyncLog;
import com.bcsport.admin.entity.ihr.ErpEmployeeSyncStatus;
import com.bcsport.admin.ihrmapper.ErpEmployeeSyncLogMapper;
import com.bcsport.admin.ihrmapper.ErpEmployeeSyncStatusMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.service.ErpEmployeeSyncService;
import com.bcsport.admin.vo.ErpEmployeeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ErpEmployeeSyncServiceImpl implements ErpEmployeeSyncService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ErpEmployeeSyncStatusMapper mapper;

    @Autowired
    private ErpEmployeeSyncLogMapper syncLogMapper;

    @Autowired
    private IhrEmployeeExclusionMapper exclusionMapper;

    @Override
    public PageResult<ErpEmployeeVO> pageOnboardings(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO) {
        String startDate = null;
        String endDate = null;

        if (queryDTO != null) {
            startDate = queryDTO.getStartDate();
            endDate = queryDTO.getEndDate();
        }

        if ((startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            LocalDate today = LocalDate.now();
            startDate = today.minusDays(30).format(DATE_FMT);
            endDate = today.format(DATE_FMT);
        }

        String staffName = queryDTO != null ? queryDTO.getStaffName() : null;
        String staffNo = queryDTO != null ? queryDTO.getStaffNo() : null;
        Integer syncStatus = queryDTO != null ? queryDTO.getSyncStatus() : null;

        List<String> excludedStaffNos = exclusionMapper.selectActiveStaffNos(1);
        List<String> shopDeptIds = mapper.selectShopDepartmentIds();
        if (shopDeptIds.isEmpty()) {
            return buildPageResult(Collections.emptyList(), 0, pageQuery.getPageNum(), pageQuery.getPageSize());
        }

        long total = mapper.countOnboarding(staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, shopDeptIds);

        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        List<ErpEmployeeVO> records = mapper.selectOnboardingPage(
                staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, shopDeptIds, offset, pageSize);

        return buildPageResult(records, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ErpEmployeeVO> pageUpdates(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO) {
        String startDate = null;
        String endDate = null;

        if (queryDTO != null) {
            startDate = queryDTO.getStartDate();
            endDate = queryDTO.getEndDate();
        }

        if ((startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            LocalDate today = LocalDate.now();
            startDate = today.minusDays(30).format(DATE_FMT);
            endDate = today.format(DATE_FMT);
        }

        String staffName = queryDTO != null ? queryDTO.getStaffName() : null;
        String staffNo = queryDTO != null ? queryDTO.getStaffNo() : null;
        Integer syncStatus = queryDTO != null ? queryDTO.getSyncStatus() : null;

        List<String> excludedStaffNos = exclusionMapper.selectActiveStaffNos(1);
        List<String> shopDeptIds = mapper.selectShopDepartmentIds();
        if (shopDeptIds.isEmpty()) {
            return buildPageResult(Collections.emptyList(), 0, pageQuery.getPageNum(), pageQuery.getPageSize());
        }

        long total = mapper.countUpdate(staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, shopDeptIds);

        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        List<ErpEmployeeVO> records = mapper.selectUpdatePage(
                staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, shopDeptIds, offset, pageSize);

        return buildPageResult(records, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ErpEmployeeVO> pageLeavings(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO) {
        String startDate = null;
        String endDate = null;

        if (queryDTO != null) {
            startDate = queryDTO.getStartDate();
            endDate = queryDTO.getEndDate();
        }

        if ((startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            LocalDate today = LocalDate.now();
            startDate = today.minusDays(30).format(DATE_FMT);
            endDate = today.format(DATE_FMT);
        }

        String staffName = queryDTO != null ? queryDTO.getStaffName() : null;
        String staffNo = queryDTO != null ? queryDTO.getStaffNo() : null;
        Integer syncStatus = queryDTO != null ? queryDTO.getSyncStatus() : null;

        List<String> excludedStaffNos = exclusionMapper.selectActiveStaffNos(2);
        List<String> shopDeptIds = mapper.selectShopDepartmentIds();
        if (shopDeptIds.isEmpty()) {
            return buildPageResult(Collections.emptyList(), 0, pageQuery.getPageNum(), pageQuery.getPageSize());
        }

        long total = mapper.countLeaving(staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, shopDeptIds);

        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        List<ErpEmployeeVO> records = mapper.selectLeavingPage(
                staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, shopDeptIds, offset, pageSize);

        return buildPageResult(records, total, pageNum, pageSize);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager", rollbackFor = Exception.class)
    public void markSyncSuccess(String syncType, String employeeId, String staffName, String staffNo, Long erpObjectId) {
        ErpEmployeeSyncStatus status = new ErpEmployeeSyncStatus();
        status.setSyncType(syncType);
        status.setEmployeeId(employeeId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(1);
        status.setErpObjectId(erpObjectId);
        status.setSyncTime(new Date());
        mapper.upsertByEmployeeIdAndType(status);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager", rollbackFor = Exception.class)
    public void markSyncFailed(String syncType, String employeeId, String staffName, String staffNo, String errorMessage) {
        ErpEmployeeSyncStatus status = new ErpEmployeeSyncStatus();
        status.setSyncType(syncType);
        status.setEmployeeId(employeeId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(2);
        status.setSyncTime(new Date());
        status.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500) : errorMessage);
        mapper.upsertByEmployeeIdAndType(status);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager", rollbackFor = Exception.class)
    public void markSyncSkipped(String syncType, String employeeId, String staffName, String staffNo) {
        // 状态不降级保护：已同步成功(1)的员工不再被覆盖成已跳过(3)，避免数据重推导致已同步→已跳过
        Integer current = mapper.selectSyncStatusByKey(employeeId, syncType);
        if (current != null && current == 1) {
            return;
        }
        ErpEmployeeSyncStatus status = new ErpEmployeeSyncStatus();
        status.setSyncType(syncType);
        status.setEmployeeId(employeeId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(3);
        status.setSyncTime(new Date());
        mapper.upsertByEmployeeIdAndType(status);
    }

    // ==================== 同步日志 ====================

    @Override
    public PageResult<ErpEmployeeSyncLog> pageLogs(PageQuery pageQuery, String syncType, String staffName, String staffNo, Integer syncStatus) {
        long total = syncLogMapper.countLog(syncType, staffName, staffNo, syncStatus);
        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;
        List<ErpEmployeeSyncLog> records = syncLogMapper.selectLogPage(syncType, staffName, staffNo, syncStatus, offset, pageSize);
        return buildLogPageResult(records, total, pageNum, pageSize);
    }

    @Override
    public ErpEmployeeSyncLog getLogById(Long id) {
        return syncLogMapper.selectDetailById(id);
    }

    @Override
    public void recordLog(String syncType, String employeeId, String staffName, String staffNo,
                          Integer syncStatus, String requestBody, String responseBody, String errorMessage) {
        try {
            ErpEmployeeSyncLog log = new ErpEmployeeSyncLog();
            log.setSyncType(syncType);
            log.setEmployeeId(employeeId);
            log.setStaffName(staffName);
            log.setStaffNo(staffNo);
            log.setSyncStatus(syncStatus);
            log.setRequestBody(requestBody);
            log.setResponseBody(responseBody);
            log.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                    ? errorMessage.substring(0, 500) : errorMessage);
            syncLogMapper.insertLog(log);
        } catch (Exception e) {
            // 日志写入失败不影响主同步流程，仅打印错误日志
            log.error("[ERP同步] 写入同步日志失败: syncType={}, employeeId={}, error={}", syncType, employeeId, e.getMessage(), e);
        }
    }

    private PageResult<ErpEmployeeSyncLog> buildLogPageResult(List<ErpEmployeeSyncLog> records, long total, int pageNum, int pageSize) {
        PageResult<ErpEmployeeSyncLog> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setRecords(records);
        result.setHasPrevious(pageNum > 1);
        result.setHasNext((long) pageNum < result.getPages());
        return result;
    }

    private PageResult<ErpEmployeeVO> buildPageResult(List<ErpEmployeeVO> records, long total, int pageNum, int pageSize) {
        PageResult<ErpEmployeeVO> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setRecords(records);
        result.setHasPrevious(pageNum > 1);
        result.setHasNext((long) pageNum < result.getPages());
        return result;
    }
}
