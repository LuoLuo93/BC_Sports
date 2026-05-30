package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SysLogQueryDTO;
import com.bcsport.admin.entity.SysLog;

public interface SysLogService {

    PageResult<SysLog> pageLogs(PageQuery pageQuery, SysLogQueryDTO queryDTO);

    void saveLog(SysLog sysLog);

    int cleanLogs(int days);
}
