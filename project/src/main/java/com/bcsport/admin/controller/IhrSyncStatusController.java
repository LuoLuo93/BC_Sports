package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.config.TaskThreadPoolConfig;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.task.qywx.QywxEmployeeLifecycleSyncTask;
import com.bcsport.admin.task.qywx.QywxFullSyncTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadPoolExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/ihr")
@Api(tags = "IHR同步状态管理")
public class IhrSyncStatusController {

    @Autowired
    private QywxFullSyncTask qywxFullSyncTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/sync-status")
    @ApiOperation("查询IHR同步状态")
    public Result<SyncStatusVO> getSyncStatus() {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setSyncing(IhrEmployeeTask.isSyncing());
        Date startTime = IhrEmployeeTask.getSyncStartTime();
        if (startTime != null) {
            vo.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
            // 计算已运行时间（秒）
            long elapsedSeconds = (System.currentTimeMillis() - startTime.getTime()) / 1000;
            vo.setElapsedSeconds(elapsedSeconds);
        }
        return Result.success(vo);
    }

    @GetMapping("/qywx-sync-status")
    @ApiOperation("查询企微同步状态")
    public Result<SyncStatusVO> getQywxSyncStatus() {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setSyncing(QywxEmployeeLifecycleSyncTask.isSyncing());
        return Result.success(vo);
    }

    @PostMapping("/qywx-full-sync")
    @ApiOperation("一键同步企微信息")
    @RequiresPermissions("ihr:onboarding:sync")
    public Result<?> qywxFullSync() {
        if (QywxFullSyncTask.isSyncing()) {
            return Result.error("企微一键同步正在进行中，请等待完成");
        }
        taskThreadPool.execute(() -> qywxFullSyncTask.syncAll());
        return Result.success("企微一键同步已触发");
    }

    @GetMapping("/qywx-full-sync-status")
    @ApiOperation("查询企微一键同步状态")
    public Result<SyncStatusVO> getQywxFullSyncStatus() {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setSyncing(QywxFullSyncTask.isSyncing());
        return Result.success(vo);
    }

    @Data
    public static class SyncStatusVO {
        private boolean syncing;
        private String startTime;
        private long elapsedSeconds;
    }
}
