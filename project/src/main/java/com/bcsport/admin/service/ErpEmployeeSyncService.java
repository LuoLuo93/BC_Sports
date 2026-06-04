package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpEmployeeQueryDTO;
import com.bcsport.admin.vo.ErpEmployeeVO;

public interface ErpEmployeeSyncService {

    PageResult<ErpEmployeeVO> pageOnboardings(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO);

    PageResult<ErpEmployeeVO> pageUpdates(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO);

    PageResult<ErpEmployeeVO> pageLeavings(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO);

    void markSyncSuccess(String syncType, String employeeId, String staffName, String staffNo, Long erpObjectId);

    void markSyncFailed(String syncType, String employeeId, String staffName, String staffNo, String errorMessage);

    void markSyncSkipped(String syncType, String employeeId, String staffName, String staffNo);
}
