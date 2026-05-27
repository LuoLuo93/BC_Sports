package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.IhrEmployeeOnboardingQueryDTO;
import com.bcsport.admin.service.IhrEmployeeOnboardingService;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.task.qywx.QywxNewEmployeeSyncTask;
import com.bcsport.admin.task.qywx.QywxEmployeeLifecycleSyncTask;
import com.bcsport.admin.vo.IhrEmployeeOnboardingVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ihr-onboarding")
@Api(tags = "IHR入职员工管理")
public class IhrEmployeeOnboardingController {

    @Autowired
    private IhrEmployeeOnboardingService onboardingService;

    @Autowired
    private IhrEmployeeTask ihrEmployeeTask;

    @Autowired
    private QywxEmployeeLifecycleSyncTask qywxEmployeeLifecycleSyncTask;

    @Autowired
    private QywxNewEmployeeSyncTask qywxNewEmployeeSyncTask;

    @GetMapping("/page")
    @ApiOperation("分页查询入职员工")
    @RequiresPermissions("ihr:onboarding:query")
    public Result<PageResult<IhrEmployeeOnboardingVO>> page(PageQuery pageQuery, IhrEmployeeOnboardingQueryDTO queryDTO) {
        PageResult<IhrEmployeeOnboardingVO> result = onboardingService.pageOnboardings(pageQuery, queryDTO);
        return Result.success(result);
    }

    @PostMapping("/sync-ihr")
    @ApiOperation("手动触发IHR员工同步")
    @RequiresPermissions("ihr:onboarding:sync")
    public Result<?> syncIhr() {
        log.info("手动触发IHR员工同步");

        synchronized (IhrEmployeeTask.class) {
            if (IhrEmployeeTask.isSyncing()) {
                log.warn("同步正在进行中，请勿重复操作");
                return Result.error("同步正在进行中，请稍后再试");
            }

            IhrEmployeeTask.setSyncing(true);
            IhrEmployeeTask.setSyncStartTime(new java.util.Date());

            new Thread(() -> {
                try {
                    ihrEmployeeTask.syncAllFromManual();
                } catch (Exception e) {
                    log.error("IHR员工同步异常", e);
                }
            }, "ihr-sync-manual").start();
        }
        return Result.success("IHR同步已触发，请稍后刷新页面查看数据");
    }

    @PostMapping("/sync-qywx")
    @ApiOperation("手动触发推送企微同步")
    @RequiresPermissions("ihr:onboarding:sync")
    public Result<?> syncQywx() {
        log.info("手动触发推送企微同步");
        if (QywxEmployeeLifecycleSyncTask.isSyncing()) {
            return Result.error("企微同步正在进行中，请稍后再试");
        }
        new Thread(() -> {
            try {
                qywxEmployeeLifecycleSyncTask.syncAll();
            } catch (Exception e) {
                log.error("企微同步异常", e);
            }
        }, "qywx-sync-manual").start();
        return Result.success("企微同步已触发，请稍后刷新页面查看同步状态");
    }

    @PostMapping("/sync-qywx/{employeesId}")
    @ApiOperation("同步单个员工到企微")
    @RequiresPermissions("ihr:onboarding:sync")
    public Result<?> syncSingleQywx(@PathVariable String employeesId) {
        log.info("手动同步单个员工到企微, employeesId={}", employeesId);
        try {
            String error = qywxNewEmployeeSyncTask.syncSingle(employeesId);
            if (error != null) {
                return Result.success(error);
            }
            return Result.success("同步成功");
        } catch (Exception e) {
            log.error("单个员工同步异常, employeesId={}", employeesId, e);
            onboardingService.markSyncFailed(employeesId, null, null, e.getMessage());
            return Result.error("同步失败: " + e.getMessage());
        }
    }
}
