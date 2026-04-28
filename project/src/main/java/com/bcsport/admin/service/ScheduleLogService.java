package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ScheduleLogQueryDTO;
import com.bcsport.admin.entity.ScheduleLog;
import com.bcsport.admin.vo.ScheduleLogVO;

public interface ScheduleLogService extends IService<ScheduleLog> {

    PageResult<ScheduleLogVO> pageLogs(PageQuery pageQuery, ScheduleLogQueryDTO queryDTO);

    void saveLog(ScheduleLog log);

    void cleanOldLogs(int keepDays);
}
