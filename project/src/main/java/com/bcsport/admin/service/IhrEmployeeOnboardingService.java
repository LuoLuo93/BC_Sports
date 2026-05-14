package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeOnboardingQueryDTO;
import com.bcsport.admin.vo.IhrEmployeeOnboardingVO;

public interface IhrEmployeeOnboardingService {

    PageResult<IhrEmployeeOnboardingVO> pageOnboardings(PageQuery pageQuery, IhrEmployeeOnboardingQueryDTO queryDTO);

    void markSyncSuccess(String employeesId, String staffName, String staffNo);

    void markSyncFailed(String employeesId, String staffName, String staffNo, String errorMessage);

    void markSyncSkipped(String employeesId, String staffName, String staffNo);
}
