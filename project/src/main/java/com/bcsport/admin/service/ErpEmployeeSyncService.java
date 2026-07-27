package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpEmployeeQueryDTO;
import com.bcsport.admin.entity.ihr.ErpEmployeeSyncLog;
import com.bcsport.admin.vo.ErpEmployeeVO;

public interface ErpEmployeeSyncService {

    PageResult<ErpEmployeeVO> pageOnboardings(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO);

    PageResult<ErpEmployeeVO> pageUpdates(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO);

    PageResult<ErpEmployeeVO> pageLeavings(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO);

    void markSyncSuccess(String syncType, String employeeId, String staffName, String staffNo, Long erpObjectId);

    void markSyncFailed(String syncType, String employeeId, String staffName, String staffNo, String errorMessage);

    void markSyncSkipped(String syncType, String employeeId, String staffName, String staffNo);

    // ==================== 同步日志 ====================

    /**
     * 分页查询同步日志
     */
    PageResult<ErpEmployeeSyncLog> pageLogs(PageQuery pageQuery, String syncType, String staffName, String staffNo, Integer syncStatus);

    /**
     * 按 id 查询日志详情（含完整请求/响应体）
     */
    ErpEmployeeSyncLog getLogById(Long id);

    /**
     * 记录一条同步日志（独立事务，失败不影响主同步流程）
     */
    void recordLog(String syncType, String employeeId, String staffName, String staffNo,
                   Integer syncStatus, String requestBody, String responseBody, String errorMessage);
}
