package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ErpEmployeeQueryDTO;
import com.bcsport.admin.service.ErpEmployeeSyncService;
import com.bcsport.admin.task.erp.ErpEmployeeSyncTask;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.vo.ErpEmployeeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RestController
@RequestMapping("/api/erp-employee")
@Api(tags = "ERP人员管理")
public class ErpEmployeeController {

    private static final Set<String> VALID_SYNC_TYPES = new HashSet<>(Arrays.asList("ONBOARDING", "UPDATE", "LEAVING"));

    @Autowired
    private ErpEmployeeSyncService erpSyncService;

    @Autowired
    private IhrEmployeeTask ihrEmployeeTask;

    @Autowired
    private ErpEmployeeSyncTask erpSyncTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/onboarding/page")
    @ApiOperation("分页查询入职员工")
    @RequiresPermissions("erp:employee:query")
    public Result<PageResult<ErpEmployeeVO>> onboardingPage(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO) {
        PageResult<ErpEmployeeVO> result = erpSyncService.pageOnboardings(pageQuery, queryDTO);
        return Result.success(result);
    }

    @GetMapping("/update/page")
    @ApiOperation("分页查询变更员工")
    @RequiresPermissions("erp:employee:query")
    public Result<PageResult<ErpEmployeeVO>> updatePage(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO) {
        PageResult<ErpEmployeeVO> result = erpSyncService.pageUpdates(pageQuery, queryDTO);
        return Result.success(result);
    }

    @GetMapping("/leaving/page")
    @ApiOperation("分页查询离职员工")
    @RequiresPermissions("erp:employee:query")
    public Result<PageResult<ErpEmployeeVO>> leavingPage(PageQuery pageQuery, ErpEmployeeQueryDTO queryDTO) {
        PageResult<ErpEmployeeVO> result = erpSyncService.pageLeavings(pageQuery, queryDTO);
        return Result.success(result);
    }

    @PostMapping("/sync-ihr")
    @ApiOperation("手动触发IHR员工同步")
    @RequiresPermissions("erp:employee:sync")
    public Result<?> syncIhr() {
        log.info("手动触发IHR员工同步(ERP页面)");

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

    @PostMapping("/sync-erp")
    @ApiOperation("手动触发ERP同步")
    @RequiresPermissions("erp:employee:sync")
    public Result<?> syncErp() {
        log.info("手动触发ERP同步");
        if (ErpEmployeeSyncTask.isSyncing()) {
            return Result.error("ERP同步正在进行中，请稍后再试");
        }
        taskThreadPool.execute(() -> {
            try {
                erpSyncTask.syncAll();
            } catch (Exception e) {
                log.error("ERP同步异常", e);
            }
        });
        return Result.success("ERP同步已触发，请稍后刷新页面查看同步状态");
    }

    @GetMapping("/sync-status")
    @ApiOperation("查询ERP同步状态")
    public Result<SyncStatusVO> getSyncStatus() {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setSyncing(ErpEmployeeSyncTask.isSyncing());
        return Result.success(vo);
    }

    @PostMapping("/sync-erp/{syncType}/{employeeId}")
    @ApiOperation("同步单个员工到ERP")
    @RequiresPermissions("erp:employee:sync")
    public Result<?> syncSingleErp(@PathVariable String syncType, @PathVariable String employeeId) {
        if (!VALID_SYNC_TYPES.contains(syncType)) {
            return Result.error("无效的同步类型: " + syncType);
        }
        log.info("手动同步单个员工到ERP, syncType={}, employeeId={}", syncType, employeeId);
        try {
            String error = erpSyncTask.syncSingle(syncType, employeeId);
            if (error != null) {
                return Result.error(error);
            }
            return Result.success("同步成功");
        } catch (Exception e) {
            log.error("单个员工ERP同步异常, syncType={}, employeeId={}", syncType, employeeId, e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @Data
    @ApiModel("同步状态")
    public static class SyncStatusVO {
        @ApiModelProperty("是否同步中")
        private boolean syncing;
    }
}
