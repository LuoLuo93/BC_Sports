package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.HkEmployeeQueryDTO;
import com.bcsport.admin.service.HkPersonnelSyncService;
import com.bcsport.admin.task.hkerp.HkPersonnelSyncTask;
import com.bcsport.admin.vo.ErpEmployeeVO;
import com.bcsport.admin.vo.HkSyncStatsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 旧版HK ERP 同步管理
 * <p>
 * 数据源：BC_SPORTS_IHR（终端店铺部门过滤），写入：HKERP Bas_Personnel（直写）。
 * 与伯俊 ERP 链路共用员工数据，仅写入目标和同步状态独立。
 */
@Slf4j
@RestController
@RequestMapping("/api/hk-personnel")
@Api(tags = "HK ERP同步管理")
public class HkPersonnelSyncController {

    private static final Set<String> VALID_SYNC_TYPES = new HashSet<>(Arrays.asList("HK_ONBOARDING", "HK_UPDATE", "HK_LEAVING"));

    @Autowired
    private HkPersonnelSyncTask hkPersonnelSyncTask;

    @Autowired
    private HkPersonnelSyncService hkPersonnelSyncService;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    // ==================== 分页查询 ====================

    @GetMapping("/onboarding/page")
    @ApiOperation("分页查询入职员工（HK链路）")
    @RequiresPermissions("hk:personnel:query")
    public Result<PageResult<ErpEmployeeVO>> onboardingPage(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO) {
        return Result.success(hkPersonnelSyncService.pageHkOnboardings(pageQuery, queryDTO));
    }

    @GetMapping("/update/page")
    @ApiOperation("分页查询变更员工（HK链路）")
    @RequiresPermissions("hk:personnel:query")
    public Result<PageResult<ErpEmployeeVO>> updatePage(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO) {
        return Result.success(hkPersonnelSyncService.pageHkUpdates(pageQuery, queryDTO));
    }

    @GetMapping("/leaving/page")
    @ApiOperation("分页查询离职员工（HK链路）")
    @RequiresPermissions("hk:personnel:query")
    public Result<PageResult<ErpEmployeeVO>> leavingPage(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO) {
        return Result.success(hkPersonnelSyncService.pageHkLeavings(pageQuery, queryDTO));
    }

    // ==================== 手动触发 ====================

    @PostMapping("/sync-onboarding")
    @ApiOperation("手动触发HK ERP入职同步")
    @RequiresPermissions("hk:personnel:sync")
    public Result<?> syncOnboarding() {
        log.info("手动触发HKERP新员工入职同步");
        if (HkPersonnelSyncTask.isOnboardingSyncing()) {
            return Result.error("入职同步正在进行中，请稍后再试");
        }
        taskThreadPool.execute(() -> {
            try {
                hkPersonnelSyncTask.syncOnboarding();
            } catch (Exception e) {
                log.error("HKERP入职同步异常", e);
            }
        });
        return Result.success("入职同步已触发，请稍后刷新查看结果");
    }

    @PostMapping("/sync-update")
    @ApiOperation("手动触发HK ERP变更与离职同步")
    @RequiresPermissions("hk:personnel:sync")
    public Result<?> syncUpdate() {
        log.info("手动触发HKERP员工变更与离职同步");
        if (HkPersonnelSyncTask.isUpdateSyncing()) {
            return Result.error("变更与离职同步正在进行中，请稍后再试");
        }
        taskThreadPool.execute(() -> {
            try {
                hkPersonnelSyncTask.syncUpdate();
            } catch (Exception e) {
                log.error("HKERP变更与离职同步异常", e);
            }
        });
        return Result.success("变更与离职同步已触发，请稍后刷新查看结果");
    }

    @PostMapping("/sync-erp/{syncType}/{employeeId}")
    @ApiOperation("同步单个员工到HK ERP")
    @RequiresPermissions("hk:personnel:sync")
    public Result<?> syncSingleErp(@PathVariable String syncType, @PathVariable String employeeId) {
        if (!VALID_SYNC_TYPES.contains(syncType)) {
            return Result.error("无效的同步类型: " + syncType);
        }
        log.info("手动同步单个员工到HK ERP, syncType={}, employeeId={}", syncType, employeeId);
        try {
            String error = hkPersonnelSyncTask.syncSingle(syncType, employeeId);
            if (error != null) {
                return Result.error(error);
            }
            return Result.success("同步成功");
        } catch (Exception e) {
            log.error("单个员工HK ERP同步异常, syncType={}, employeeId={}", syncType, employeeId, e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @GetMapping("/sync-status")
    @ApiOperation("查询HK同步状态")
    public Result<SyncStatusVO> getSyncStatus() {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setLifecycleSyncing(HkPersonnelSyncTask.isLifecycleSyncing());
        vo.setOnboardingSyncing(HkPersonnelSyncTask.isOnboardingSyncing());
        vo.setUpdateSyncing(HkPersonnelSyncTask.isUpdateSyncing());
        vo.setLastOnboardingStats(HkPersonnelSyncTask.getLastOnboardingStats());
        vo.setLastUpdateStats(HkPersonnelSyncTask.getLastUpdateStats());
        vo.setLastOnboardingTime(HkPersonnelSyncTask.getLastOnboardingTime());
        vo.setLastUpdateTime(HkPersonnelSyncTask.getLastUpdateTime());
        vo.setLastLifecycleTime(HkPersonnelSyncTask.getLastLifecycleTime());
        return Result.success(vo);
    }

    @Data
    @ApiModel("HK ERP同步状态")
    public static class SyncStatusVO {
        private boolean lifecycleSyncing;
        private boolean onboardingSyncing;
        private boolean updateSyncing;
        private HkSyncStatsVO lastOnboardingStats;
        private HkSyncStatsVO lastUpdateStats;
        private Date lastOnboardingTime;
        private Date lastUpdateTime;
        private Date lastLifecycleTime;
    }
}
