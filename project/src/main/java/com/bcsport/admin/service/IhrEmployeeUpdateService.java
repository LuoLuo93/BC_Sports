package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeUpdateQueryDTO;
import com.bcsport.admin.entity.ihr.IhrEmployeeUpdateStatus;
import com.bcsport.admin.vo.IhrEmployeeUpdateVO;

public interface IhrEmployeeUpdateService {

    PageResult<IhrEmployeeUpdateVO> pageUpdates(PageQuery pageQuery, IhrEmployeeUpdateQueryDTO queryDTO);

    void markSyncSuccess(String staffId, String staffName, String staffNo);

    void markSyncFailed(String staffId, String staffName, String staffNo, String errorMessage);

    void markSyncSkipped(String staffId, String staffName, String staffNo);
}
