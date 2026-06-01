package com.bcsport.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.SysLog;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.entity.sticker.StickerPrintOrder;
import com.bcsport.admin.mapper.SysLogMapper;
import com.bcsport.admin.mapper.UserMapper;
import com.bcsport.admin.service.sticker.StickerPrintService;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.task.erp.ErpEmployeeSyncTask;
import com.bcsport.admin.task.qywx.QywxEmployeeLifecycleSyncTask;
import com.bcsport.admin.task.qywx.QywxFullSyncTask;
import com.bcsport.admin.task.nxcrm.NxcrmTagIncrementSyncTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@Api(tags = "数据驾驶舱")
public class DashboardController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/stats")
    @ApiOperation("核心指标统计")
    public Result<StatsVO> stats() {
        StatsVO vo = new StatsVO();

        // 用户总数
        vo.setUserCount(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));

        // 在线用户数（Redis session keys）
        try {
            Set<String> keys = redisTemplate.keys("auth:session:*");
            vo.setOnlineUserCount(keys != null ? keys.size() : 0);
        } catch (Exception e) {
            vo.setOnlineUserCount(0);
        }

        // 今日操作数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        vo.setTodayLogCount(sysLogMapper.selectCount(
                new LambdaQueryWrapper<SysLog>().ge(SysLog::getOperationTime, todayStart)));

        return Result.success(vo);
    }

    @GetMapping("/sync-status")
    @ApiOperation("同步任务状态")
    public Result<SyncStatusVO> syncStatus() {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setIhrSyncing(IhrEmployeeTask.isSyncing());
        vo.setErpSyncing(ErpEmployeeSyncTask.isSyncing());
        vo.setQywxSyncing(QywxEmployeeLifecycleSyncTask.isSyncing() || QywxFullSyncTask.isSyncing());
        vo.setNxcrmSyncing(NxcrmTagIncrementSyncTask.isSyncing());
        return Result.success(vo);
    }

    @GetMapping("/system-health")
    @ApiOperation("系统健康状态")
    public Result<SystemHealthVO> systemHealth() {
        SystemHealthVO vo = new SystemHealthVO();

        // Redis 连接状态
        try {
            redisTemplate.hasKey("health:check");
            vo.setRedisOk(true);
        } catch (Exception e) {
            vo.setRedisOk(false);
        }

        // JVM 内存
        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;
        vo.setMemoryUsedMb(usedMem / (1024 * 1024));
        vo.setMemoryTotalMb(totalMem / (1024 * 1024));
        vo.setMemoryUsagePercent(Math.round(usedMem * 100.0 / totalMem));

        // CPU 核心数
        vo.setCpuCores(rt.availableProcessors());

        return Result.success(vo);
    }

    @Data
    public static class StatsVO {
        private long userCount;
        private int onlineUserCount;
        private long todayLogCount;
    }

    @Data
    public static class SyncStatusVO {
        private boolean ihrSyncing;
        private boolean erpSyncing;
        private boolean qywxSyncing;
        private boolean nxcrmSyncing;
    }

    @Data
    public static class SystemHealthVO {
        private boolean redisOk;
        private long memoryUsedMb;
        private long memoryTotalMb;
        private long memoryUsagePercent;
        private int cpuCores;
    }
}
