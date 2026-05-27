package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.task.qywx.QywxEmployeeLifecycleSyncTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/ihr")
@Api(tags = "IHR同步状态管理")
public class IhrSyncStatusController {

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

    @Data
    public static class SyncStatusVO {
        private boolean syncing;
        private String startTime;
        private long elapsedSeconds;
    }
}
