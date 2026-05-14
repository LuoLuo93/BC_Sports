package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeLeavingQueryDTO;
import com.bcsport.admin.vo.IhrEmployeeLeavingVO;

public interface IhrEmployeeLeavingService {

    PageResult<IhrEmployeeLeavingVO> pageLeavings(PageQuery pageQuery, IhrEmployeeLeavingQueryDTO queryDTO);

    void markSyncSuccess(String employeeId, String staffName, String staffNo);

    void markSyncFailed(String employeeId, String staffName, String staffNo, String errorMessage);

    void markSyncSkipped(String employeeId, String staffName, String staffNo);
}
