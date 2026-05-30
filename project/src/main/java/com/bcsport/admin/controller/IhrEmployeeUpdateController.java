package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.IhrEmployeeUpdateQueryDTO;
import com.bcsport.admin.service.IhrEmployeeUpdateService;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.task.qywx.QywxEmployeeUpdateSyncTask;
import com.bcsport.admin.vo.IhrEmployeeUpdateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RestController
@RequestMapping("/api/ihr-update")
@Api(tags = "IHR调整员工管理")
public class IhrEmployeeUpdateController {

    @Autowired
    private IhrEmployeeUpdateService updateService;

    @Autowired
    private IhrEmployeeTask ihrEmployeeTask;

    @Autowired
    private QywxEmployeeUpdateSyncTask qywxEmployeeUpdateSyncTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/page")
    @ApiOperation("分页查询调整员工")
    @RequiresPermissions("ihr:update:query")
    public Result<PageResult<IhrEmployeeUpdateVO>> page(PageQuery pageQuery, IhrEmployeeUpdateQueryDTO queryDTO) {
        PageResult<IhrEmployeeUpdateVO> result = updateService.pageUpdates(pageQuery, queryDTO);
        return Result.success(result);
    }

    @PostMapping("/sync-ihr")
    @ApiOperation("手动触发IHR员工同步")
    @RequiresPermissions("ihr:update:sync")
    public Result<?> syncIhr() {
        log.info("手动触发IHR员工同步(调整管理)");

        synchronized (IhrEmployeeTask.class) {
            if (IhrEmployeeTask.isSyncing()) {
                log.warn("同步正在进行中，请勿重复操作");
                return Result.error("同步正在进行中，请稍后再试");
            }

            IhrEmployeeTask.setSyncing(true);
            IhrEmployeeTask.setSyncStartTime(new java.util.Date());

            taskThreadPool.execute(() -> {
                try {
                    ihrEmployeeTask.syncAllFromManual();
                } catch (Exception e) {
                    log.error("IHR员工同步异常", e);
                }
            });
        }
        return Result.success("IHR同步已触发，请稍后刷新页面查看数据");
    }

    @PostMapping("/sync-qywx/{staffId}")
    @ApiOperation("同步单个调整员工到企微")
    @RequiresPermissions("ihr:update:sync")
    public Result<?> syncSingleQywx(@PathVariable String staffId) {
        log.info("手动同步单个调整员工到企微, staffId={}", staffId);
        try {
            String error = qywxEmployeeUpdateSyncTask.syncSingle(staffId);
            if (error != null) {
                return Result.success(error);
            }
            return Result.success("同步成功");
        } catch (Exception e) {
            log.error("单个调整员工同步异常, staffId={}", staffId, e);
            updateService.markSyncFailed(staffId, null, null, e.getMessage());
            return Result.error("同步失败: " + e.getMessage());
        }
    }
}
