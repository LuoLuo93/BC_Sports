package com.bcsport.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.JenkinsJobState;
import com.bcsport.admin.mapper.JenkinsJobStateMapper;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.task.sys.JenkinsMonitorTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jenkins任务状态监控（运维监控-Jenkins监控明细页）
 * 数据由定时任务 sys.jenkins.monitor 全量同步到 BC_SPORTS_JENKINS_JOB_STATE（含停用任务，页面默认只显示启用）。
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/jenkins")
@Api(tags = "Jenkins监控")
public class MonitorJenkinsController {

    private static final String CONFIG_BASE_URL = "jenkins.monitor.baseUrl";
    private static final String DEFAULT_BASE_URL = "http://192.168.5.151:8085";

    @Autowired
    private JenkinsJobStateMapper jenkinsJobStateMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private JenkinsMonitorTask jenkinsMonitorTask;

    /**
     * 分页查询任务状态：buildable 过滤启用/停用（页面默认启用），keyword 按任务名模糊
     */
    @GetMapping("/page")
    @ApiOperation("分页查询Jenkins任务状态")
    @RequiresPermissions("monitor:jenkins:query")
    public Result<PageResult<JenkinsJobState>> page(PageQuery pageQuery,
                                                    @RequestParam(required = false) Integer buildable,
                                                    @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<JenkinsJobState> wrapper = new LambdaQueryWrapper<>();
        if (buildable != null) {
            wrapper.eq(JenkinsJobState::getBuildable, buildable);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(JenkinsJobState::getJobName, keyword.trim());
        }
        // 启用在前,任务名升序
        wrapper.orderByDesc(JenkinsJobState::getBuildable).orderByAsc(JenkinsJobState::getJobName);
        Page<JenkinsJobState> result = jenkinsJobStateMapper.selectPage(pageQuery.toPage(), wrapper);
        return Result.success(PageResult.of(result));
    }

    /**
     * 页头信息：Jenkins 控制台地址 + 启用/停用数量 + 最近同步时间
     */
    @GetMapping("/info")
    @ApiOperation("Jenkins地址与统计信息")
    @RequiresPermissions("monitor:jenkins:query")
    public Result<Map<String, Object>> info() {
        List<JenkinsJobState> all = jenkinsJobStateMapper.selectList(null);
        int enabled = 0;
        Date lastSync = null;
        for (JenkinsJobState row : all) {
            if (row.getBuildable() != null && row.getBuildable() == 1) {
                enabled++;
            }
            if (row.getUpdateTime() != null && (lastSync == null || row.getUpdateTime().after(lastSync))) {
                lastSync = row.getUpdateTime();
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("baseUrl", configService.getString(CONFIG_BASE_URL, DEFAULT_BASE_URL));
        data.put("total", all.size());
        data.put("enabled", enabled);
        data.put("disabled", all.size() - enabled);
        data.put("lastSyncTime", lastSync != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastSync) : null);
        return Result.success(data);
    }

    /**
     * 立即同步一次（不等 cron）；有新失败构建会即时推送企微群
     */
    @PostMapping("/sync")
    @ApiOperation("立即同步Jenkins任务状态")
    @RequiresPermissions("monitor:jenkins:query")
    @OperLog(module = "Jenkins监控", operation = "立即同步")
    public Result<?> sync() {
        long t0 = System.currentTimeMillis();
        jenkinsMonitorTask.monitor();
        return Result.success("同步完成,耗时" + (System.currentTimeMillis() - t0) + "ms");
    }
}
