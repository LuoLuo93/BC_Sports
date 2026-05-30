package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.config.ScheduleConfig;
import com.bcsport.admin.dto.ScheduleJobDTO;
import com.bcsport.admin.dto.ScheduleJobQueryDTO;
import com.bcsport.admin.dto.ScheduleLogQueryDTO;
import com.bcsport.admin.service.ScheduleJobService;
import com.bcsport.admin.service.ScheduleLogService;
import com.bcsport.admin.task.ScheduleTaskRegistry;
import com.bcsport.admin.util.CronUtils;
import com.bcsport.admin.vo.ScheduleJobVO;
import com.bcsport.admin.vo.ScheduleLogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@Api(tags = "定时任务管理")
public class ScheduleController {

    @Autowired
    private ScheduleJobService scheduleJobService;

    @Autowired
    private ScheduleLogService scheduleLogService;

    @GetMapping("/tasks")
    @ApiOperation("获取预设任务列表")
    @RequiresPermissions("monitor:schedule:query")
    public Result<List<ScheduleTaskRegistry.TaskOption>> getTaskOptions() {
        return Result.success(ScheduleTaskRegistry.getAllTasks());
    }

    @GetMapping("/cron/validate")
    @ApiOperation("校验Cron表达式")
    @RequiresPermissions("monitor:schedule:query")
    public Result<String> validateCron(@RequestParam String cronExpression) {
        if (!CronUtils.isValid(cronExpression)) {
            return Result.paramError("Cron表达式格式错误");
        }
        String nextTime = CronUtils.getNextExecution(cronExpression) != null
                ? CronUtils.getNextExecution(cronExpression).toString().replace("T", " ")
                : "无法计算";
        return Result.success("校验通过", nextTime);
    }

    @GetMapping("/job/page")
    @ApiOperation("分页查询定时任务")
    @RequiresPermissions("monitor:schedule:query")
    public Result<PageResult<ScheduleJobVO>> pageJobs(PageQuery pageQuery, ScheduleJobQueryDTO queryDTO) {
        return Result.success(scheduleJobService.pageJobs(pageQuery, queryDTO));
    }

    @GetMapping("/job/{id}")
    @ApiOperation("查询任务详情")
    @RequiresPermissions("monitor:schedule:query")
    public Result<ScheduleJobVO> getJob(@PathVariable String id) {
        ScheduleJobVO vo = scheduleJobService.getJobVOById(id);
        if (vo == null) {
            return Result.notFound("任务不存在");
        }
        return Result.success(vo);
    }

    @PostMapping("/job")
    @ApiOperation("新增定时任务")
    @RequiresPermissions("monitor:schedule:add")
    public Result<?> addJob(@Valid @RequestBody ScheduleJobDTO dto) {
        boolean success = scheduleJobService.addJob(dto);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    @PutMapping("/job/{id}")
    @ApiOperation("修改定时任务")
    @RequiresPermissions("monitor:schedule:edit")
    public Result<?> updateJob(@PathVariable String id, @Valid @RequestBody ScheduleJobDTO dto) {
        dto.setId(id);
        boolean success = scheduleJobService.updateJob(dto);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    @DeleteMapping("/job/{id}")
    @ApiOperation("删除定时任务")
    @RequiresPermissions("monitor:schedule:delete")
    public Result<?> deleteJob(@PathVariable String id) {
        boolean success = scheduleJobService.deleteJob(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    @PutMapping("/job/{id}/pause")
    @ApiOperation("暂停任务")
    @RequiresPermissions("monitor:schedule:edit")
    public Result<?> pauseJob(@PathVariable String id) {
        scheduleJobService.pauseJob(id);
        return Result.success("已暂停");
    }

    @PutMapping("/job/{id}/resume")
    @ApiOperation("恢复任务")
    @RequiresPermissions("monitor:schedule:edit")
    public Result<?> resumeJob(@PathVariable String id) {
        scheduleJobService.resumeJob(id);
        return Result.success("已恢复运行");
    }

    @PostMapping("/job/{id}/run")
    @ApiOperation("手动执行一次")
    @RequiresPermissions("monitor:schedule:edit")
    public Result<?> runOnce(@PathVariable String id, @RequestBody(required = false) Map<String, String> params) {
        // 防止同一任务重复触发
        if (ScheduleConfig.isJobRunning(id)) {
            return Result.error("任务「" + ScheduleConfig.getJobName(id) + "」正在执行中，请等待完成后再试");
        }
        scheduleJobService.runOnce(id, params);
        return Result.success("已触发执行");
    }

    @GetMapping("/run-status")
    @ApiOperation("查询手动执行状态")
    @RequiresPermissions("monitor:schedule:query")
    public Result<RunStatusVO> getRunStatus(@RequestParam(required = false) String jobId) {
        RunStatusVO vo = new RunStatusVO();
        if (jobId != null && !jobId.isEmpty()) {
            // 查询指定任务状态
            vo.setRunning(ScheduleConfig.isJobRunning(jobId));
            vo.setJobName(ScheduleConfig.getJobName(jobId));
            vo.setElapsedSeconds(ScheduleConfig.getJobElapsedSeconds(jobId));
        } else {
            // 查询全局状态
            vo.setRunning(ScheduleConfig.isAnyManualRunning());
            vo.setJobName(ScheduleConfig.getFirstRunningJobName());
            vo.setElapsedSeconds(0);
            vo.setRunningJobIds(ScheduleConfig.getRunningJobIds());
        }
        return Result.success(vo);
    }

    @lombok.Data
    public static class RunStatusVO {
        private boolean running;
        private String jobName;
        private long elapsedSeconds;
        private java.util.Set<String> runningJobIds;
    }

    @GetMapping("/log/page")
    @ApiOperation("分页查询执行日志")
    @RequiresPermissions("monitor:schedule:query")
    public Result<PageResult<ScheduleLogVO>> pageLogs(PageQuery pageQuery, ScheduleLogQueryDTO queryDTO) {
        return Result.success(scheduleLogService.pageLogs(pageQuery, queryDTO));
    }

    @DeleteMapping("/log/clean")
    @ApiOperation("清理历史日志")
    @RequiresPermissions("monitor:schedule:delete")
    public Result<?> cleanLogs(@RequestParam(defaultValue = "30") int keepDays) {
        if (keepDays < 7) {
            return Result.paramError("保留天数不能少于7天");
        }
        scheduleLogService.cleanOldLogs(keepDays);
        return Result.success("清理完成");
    }
}
