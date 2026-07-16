package com.bcsport.admin.service.impl;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeUpdateQueryDTO;
import com.bcsport.admin.entity.ihr.IhrEmployeeUpdateStatus;
import com.bcsport.admin.ihrmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.ihrmapper.IhrEmployeeUpdateStatusMapper;
import com.bcsport.admin.service.IhrEmployeeUpdateService;
import com.bcsport.admin.vo.IhrEmployeeUpdateVO;
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
public class IhrEmployeeUpdateServiceImpl implements IhrEmployeeUpdateService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private IhrEmployeeUpdateStatusMapper mapper;

    @Autowired
    private IhrEmployeeExclusionMapper exclusionMapper;

    @Override
    public PageResult<IhrEmployeeUpdateVO> pageUpdates(PageQuery pageQuery, IhrEmployeeUpdateQueryDTO queryDTO) {
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

        long total = mapper.countUpdate(staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos);

        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        List<IhrEmployeeUpdateVO> records = mapper.selectUpdatePage(
                staffName, staffNo, syncStatus, startDate, endDate, excludedStaffNos, offset, pageSize);

        PageResult<IhrEmployeeUpdateVO> result = new PageResult<>();
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
    @Transactional(transactionManager = "ihrTransactionManager", rollbackFor = Exception.class)
    public void markSyncSuccess(String staffId, String staffName, String staffNo) {
        IhrEmployeeUpdateStatus status = new IhrEmployeeUpdateStatus();
        status.setStaffId(staffId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(1);
        status.setSyncTime(new Date());
        mapper.upsertByStaffId(status);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager", rollbackFor = Exception.class)
    public void markSyncFailed(String staffId, String staffName, String staffNo, String errorMessage) {
        IhrEmployeeUpdateStatus status = new IhrEmployeeUpdateStatus();
        status.setStaffId(staffId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(2);
        status.setSyncTime(new Date());
        status.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500) : errorMessage);
        mapper.upsertByStaffId(status);
    }

    @Override
    @Transactional(transactionManager = "ihrTransactionManager", rollbackFor = Exception.class)
    public void markSyncSkipped(String staffId, String staffName, String staffNo) {
        // 状态不降级保护：已同步成功(1)的员工不再被覆盖成已跳过(3)
        Integer current = mapper.selectSyncStatusByStaffId(staffId);
        if (current != null && current == 1) {
            return;
        }
        IhrEmployeeUpdateStatus status = new IhrEmployeeUpdateStatus();
        status.setStaffId(staffId);
        status.setStaffName(staffName);
        status.setStaffNo(staffNo);
        status.setSyncStatus(3);
        status.setSyncTime(new Date());
        mapper.upsertByStaffId(status);
    }
}
