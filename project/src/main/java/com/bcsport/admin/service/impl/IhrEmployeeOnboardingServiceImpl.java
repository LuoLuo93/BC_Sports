package com.bcsport.admin.service.impl;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeOnboardingQueryDTO;
import com.bcsport.admin.entity.ihr.IhrEmployeeOnboardingStatus;
import com.bcsport.admin.ihrmapper.IhrEmployeeOnboardingStatusMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.service.IhrEmployeeOnboardingService;
import com.bcsport.admin.vo.IhrEmployeeOnboardingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class IhrEmployeeOnboardingServiceImpl implements IhrEmployeeOnboardingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private IhrEmployeeOnboardingStatusMapper mapper;

    @Autowired
    private IhrEmployeeExclusionMapper exclusionMapper;

    @Override
    public PageResult<IhrEmployeeOnboardingVO> pageOnboardings(PageQuery pageQuery, IhrEmployeeOnboardingQueryDTO queryDTO) {
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

        long total = mapper.countOnboarding(staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos);

        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        List<IhrEmployeeOnboardingVO> records = mapper.selectOnboardingPage(
                staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, offset, pageSize);

        PageResult<IhrEmployeeOnboardingVO> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setRecords(records);
        result.setHasPrevious(pageNum > 1);
        result.setHasNext((long) pageNum < result.getPages());
        return result;
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager")
    public void markSyncSuccess(String employeesId, String staffName, String staffNo) {
        IhrEmployeeOnboardingStatus status = new IhrEmployeeOnboardingStatus();
        status.setEmployeesId(employeesId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(1);
        status.setSyncTime(new Date());
        mapper.upsertByEmployeesId(status);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager")
    public void markSyncFailed(String employeesId, String staffName, String staffNo, String errorMessage) {
        IhrEmployeeOnboardingStatus status = new IhrEmployeeOnboardingStatus();
        status.setEmployeesId(employeesId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(2);
        status.setSyncTime(new Date());
        status.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500) : errorMessage);
        mapper.upsertByEmployeesId(status);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager")
    public void markSyncSkipped(String employeesId, String staffName, String staffNo) {
        IhrEmployeeOnboardingStatus status = new IhrEmployeeOnboardingStatus();
        status.setEmployeesId(employeesId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(3);
        status.setSyncTime(new Date());
        mapper.upsertByEmployeesId(status);
    }
}
