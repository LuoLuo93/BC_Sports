package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.config.ScheduleConfig;
import com.bcsport.admin.dto.ScheduleJobDTO;
import com.bcsport.admin.dto.ScheduleJobQueryDTO;
import com.bcsport.admin.entity.ScheduleJob;
import com.bcsport.admin.mapper.ScheduleJobMapper;
import com.bcsport.admin.service.ScheduleJobService;
import com.bcsport.admin.task.ScheduleTaskRegistry;
import org.springframework.transaction.annotation.Transactional;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.util.CronUtils;
import com.bcsport.admin.vo.ScheduleJobVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.hutool.json.JSONUtil;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScheduleJobServiceImpl extends ServiceImpl<ScheduleJobMapper, ScheduleJob> implements ScheduleJobService {

    @Autowired
    private ScheduleConfig scheduleConfig;

    @Override
    public PageResult<ScheduleJobVO> pageJobs(PageQuery pageQuery, ScheduleJobQueryDTO queryDTO) {
        Page<ScheduleJob> page = pageQuery.toPage();
        LambdaQueryWrapper<ScheduleJob> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getJobName())) {
                wrapper.like(ScheduleJob::getJobName, queryDTO.getJobName());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(ScheduleJob::getStatus, queryDTO.getStatus());
            }
            if (StringUtils.hasText(queryDTO.getModule())) {
                wrapper.eq(ScheduleJob::getModule, queryDTO.getModule());
            }
        }
        wrapper.eq(ScheduleJob::getDeleted, 0);
        wrapper.orderByAsc(ScheduleJob::getModule)
               .orderByAsc(ScheduleJob::getSort)
               .orderByDesc(ScheduleJob::getCreateTime);

        Page<ScheduleJob> result = baseMapper.selectPage(page, wrapper);
        PageResult<ScheduleJobVO> voPage = BeanCopyUtils.copyPage(PageResult.of(result), ScheduleJobVO.class);

        if (voPage.getRecords() != null) {
            for (ScheduleJobVO vo : voPage.getRecords()) {
                ScheduleTaskRegistry.TaskOption option = ScheduleTaskRegistry.getTask(vo.getTaskKey());
                if (option != null) {
                    vo.setTaskDescription(option.getDescription());
                    vo.setBeanName(option.getBeanName());
                    vo.setMethodName(option.getMethodName());
                    if (option.getParamDefs() != null) {
                        vo.setParamDefs(option.getParamDefs().stream()
                            .map(pd -> {
                                Map<String, String> m = new java.util.HashMap<>();
                                m.put("key", pd.getKey());
                                m.put("label", pd.getLabel());
                                m.put("type", pd.getType());
                                return m;
                            }).collect(java.util.stream.Collectors.toList()));
                    }
                    if (vo.getModule() == null) {
                        vo.setModule(option.getModule());
                    }
                    if (vo.getSort() == null) {
                        vo.setSort(option.getSort());
                    }
                }
                if (Integer.valueOf(1).equals(vo.getStatus()) && StringUtils.hasText(vo.getCronExpression())) {
                    try {
                        vo.setNextExecuteTime(CronUtils.getNextExecution(vo.getCronExpression()));
                    } catch (Exception e) {
                        log.debug("解析Cron表达式失败: {}", vo.getCronExpression());
                    }
                }
            }
        }
        return voPage;
    }

    @Override
    public ScheduleJobVO getJobVOById(String id) {
        ScheduleJob job = getById(id);
        if (job == null) {
            return null;
        }
        ScheduleJobVO vo = BeanCopyUtils.copy(job, ScheduleJobVO.class);
        ScheduleTaskRegistry.TaskOption option = ScheduleTaskRegistry.getTask(vo.getTaskKey());
        if (option != null) {
            vo.setTaskDescription(option.getDescription());
            vo.setBeanName(option.getBeanName());
            vo.setMethodName(option.getMethodName());
            if (option.getParamDefs() != null) {
                vo.setParamDefs(option.getParamDefs().stream()
                    .map(pd -> {
                        Map<String, String> m = new java.util.HashMap<>();
                        m.put("key", pd.getKey());
                        m.put("label", pd.getLabel());
                        m.put("type", pd.getType());
                        return m;
                    }).collect(java.util.stream.Collectors.toList()));
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addJob(ScheduleJobDTO dto) {
        if (!CronUtils.isValid(dto.getCronExpression())) {
            throw new BusinessException("Cron表达式不合法");
        }
        ScheduleTaskRegistry.TaskOption option = ScheduleTaskRegistry.getTask(dto.getTaskKey());
        if (option == null) {
            throw new BusinessException("预设任务不存在 " + dto.getTaskKey());
        }
        ScheduleJob job = BeanCopyUtils.copy(dto, ScheduleJob.class);
        if (job.getStatus() == null) {
            job.setStatus(0);
        }
        if (job.getModule() == null) {
            job.setModule(option.getModule());
        }
        if (job.getSort() == null) {
            job.setSort(option.getSort());
        }
        // 雪花ID碰撞重试（多实例部署时workerId相同可能产生重复ID）
        boolean saved = false;
        for (int i = 0; i < 3; i++) {
            try {
                job.setId(null); // 清除ID让雪花重新生成
                saved = save(job);
                break;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.warn("雪花ID碰撞，重试第{}次: {}", i + 1, e.getMessage());
            }
        }
        if (!saved) {
            throw new BusinessException("新增任务失败：主键冲突重试3次均失败");
        }
        if (Integer.valueOf(1).equals(job.getStatus())) {
            scheduleConfig.registerTask(job);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJob(ScheduleJobDTO dto) {
        if (!CronUtils.isValid(dto.getCronExpression())) {
            throw new BusinessException("Cron表达式不合法");
        }
        ScheduleTaskRegistry.TaskOption option = ScheduleTaskRegistry.getTask(dto.getTaskKey());
        if (option == null) {
            throw new BusinessException("预设任务不存在 " + dto.getTaskKey());
        }
        scheduleConfig.removeTask(dto.getId());

        // 先获取数据库中的现有值
        ScheduleJob existingJob = getById(dto.getId());
        ScheduleJob job = BeanCopyUtils.copy(dto, ScheduleJob.class);

        // 编辑时：如果传了空值，保留数据库原值，不要用预设任务覆盖
        if (job.getModule() == null) {
            if (existingJob != null && existingJob.getModule() != null) {
                job.setModule(existingJob.getModule());
            } else {
                job.setModule(option.getModule());
            }
        }
        if (job.getSort() == null) {
            if (existingJob != null && existingJob.getSort() != null) {
                job.setSort(existingJob.getSort());
            } else {
                job.setSort(option.getSort());
            }
        }
        boolean updated = updateById(job);
        if (updated && Integer.valueOf(1).equals(job.getStatus())) {
            scheduleConfig.registerTask(job);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteJob(String id) {
        scheduleConfig.removeTask(id);
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pauseJob(String id) {
        ScheduleJob job = getById(id);
        if (job == null) {
            throw new BusinessException("任务不存在");
        }
        scheduleConfig.removeTask(id);
        job.setStatus(0);
        return updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resumeJob(String id) {
        ScheduleJob job = getById(id);
        if (job == null) {
            throw new BusinessException("任务不存在");
        }
        job.setStatus(1);
        scheduleConfig.registerTask(job);
        return updateById(job);
    }

    @Override
    public void runOnce(String id) {
        runOnce(id, null);
    }

    @Override
    public void runOnce(String id, Map<String, String> params) {
        ScheduleJob job = getById(id);
        if (job == null) {
            throw new BusinessException("任务不存在");
        }
        if (params != null && !params.isEmpty()) {
            job.setParams(JSONUtil.toJsonStr(params));
        }
        scheduleConfig.executeJobImmediately(job);
    }

    @PostConstruct
    @Override
    public void refreshAllJobs() {
        LambdaQueryWrapper<ScheduleJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleJob::getStatus, 1).eq(ScheduleJob::getDeleted, 0);
        List<ScheduleJob> jobs = list(wrapper);
        for (ScheduleJob job : jobs) {
            try {
                scheduleConfig.registerTask(job);
            } catch (Exception e) {
                log.error("注册定时任务失败: {}", job.getJobName(), e);
            }
        }
        log.info("共注册 {} 个定时任务", jobs.size());
    }
}
