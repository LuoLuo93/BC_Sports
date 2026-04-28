package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ScheduleLogQueryDTO;
import com.bcsport.admin.entity.ScheduleLog;
import com.bcsport.admin.mapper.ScheduleLogMapper;
import com.bcsport.admin.service.ScheduleLogService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ScheduleLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ScheduleLogServiceImpl extends ServiceImpl<ScheduleLogMapper, ScheduleLog> implements ScheduleLogService {

    @Override
    public PageResult<ScheduleLogVO> pageLogs(PageQuery pageQuery, ScheduleLogQueryDTO queryDTO) {
        Page<ScheduleLog> page = pageQuery.toPage();
        LambdaQueryWrapper<ScheduleLog> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getJobId())) {
                wrapper.eq(ScheduleLog::getJobId, queryDTO.getJobId());
            }
            if (StringUtils.hasText(queryDTO.getJobName())) {
                wrapper.like(ScheduleLog::getJobName, queryDTO.getJobName());
            }
            if (StringUtils.hasText(queryDTO.getTriggerType())) {
                wrapper.eq(ScheduleLog::getTriggerType, queryDTO.getTriggerType());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(ScheduleLog::getStatus, queryDTO.getStatus());
            }
        }
        wrapper.orderByDesc(ScheduleLog::getExecuteTime);
        Page<ScheduleLog> result = baseMapper.selectPage(page, wrapper);
        return BeanCopyUtils.copyPage(PageResult.of(result), ScheduleLogVO.class);
    }

    @Override
    public void saveLog(ScheduleLog log) {
        baseMapper.insert(log);
    }

    @Override
    public void cleanOldLogs(int keepDays) {
        baseMapper.delete(new LambdaQueryWrapper<ScheduleLog>()
                .lt(ScheduleLog::getExecuteTime, LocalDateTime.now().minusDays(keepDays)));
    }
}
