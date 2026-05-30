package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SysLogQueryDTO;
import com.bcsport.admin.entity.SysLog;
import com.bcsport.admin.mapper.SysLogMapper;
import com.bcsport.admin.service.SysLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysLogServiceImpl implements SysLogService {

    @Autowired
    private SysLogMapper sysLogMapper;

    @Override
    public PageResult<SysLog> pageLogs(PageQuery pageQuery, SysLogQueryDTO queryDTO) {
        IPage<SysLog> page = sysLogMapper.selectLogPage(pageQuery.toPage(), queryDTO);
        return new PageResult<>(page);
    }

    @Override
    @Async
    public void saveLog(SysLog sysLog) {
        try {
            log.info("[OperLog] 保存日志: module={}, operation={}, username={}", sysLog.getModule(), sysLog.getOperation(), sysLog.getUsername());
            int rows = sysLogMapper.insert(sysLog);
            log.info("[OperLog] 保存完成: rows={}", rows);
        } catch (Exception e) {
            log.error("[OperLog] 保存操作日志失败: module={}, operation={}, error={}", sysLog.getModule(), sysLog.getOperation(), e.getMessage(), e);
        }
    }

    @Override
    public int cleanLogs(int days) {
        return sysLogMapper.deleteBeforeDays(days);
    }
}
