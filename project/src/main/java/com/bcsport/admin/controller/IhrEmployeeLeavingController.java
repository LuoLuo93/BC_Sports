package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.IhrEmployeeLeavingQueryDTO;
import com.bcsport.admin.service.IhrEmployeeLeavingService;
import com.bcsport.admin.task.ihr.IhrEmployeeTask;
import com.bcsport.admin.task.qywx.QywxEmployeeLeaveSyncTask;
import com.bcsport.admin.vo.IhrEmployeeLeavingVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ihr-leaving")
@Api(tags = "IHR离职员工管理")
public class IhrEmployeeLeavingController {

    @Autowired
    private IhrEmployeeLeavingService leavingService;

    @Autowired
    private IhrEmployeeTask ihrEmployeeTask;

    @Autowired
    private QywxEmployeeLeaveSyncTask qywxEmployeeLeaveSyncTask;

    @GetMapping("/page")
    @ApiOperation("分页查询离职员工")
    @RequiresPermissions("ihr:leaving:query")
    public Result<PageResult<IhrEmployeeLeavingVO>> page(PageQuery pageQuery, IhrEmployeeLeavingQueryDTO queryDTO) {
        PageResult<IhrEmployeeLeavingVO> result = leavingService.pageLeavings(pageQuery, queryDTO);
        return Result.success(result);
    }

    @PostMapping("/sync-ihr")
    @ApiOperation("手动触发IHR员工同步")
    @RequiresPermissions("ihr:leaving:sync")
    public Result<?> syncIhr() {
        log.info("手动触发IHR员工同步(离职管理)");

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
            }, "ihr-sync-manual-leaving").start();
        }
        return Result.success("IHR同步已触发，请稍后刷新页面查看数据");
    }

    @PostMapping("/sync-qywx/{employeeId}")
    @ApiOperation("同步单个离职员工到企微")
    @RequiresPermissions("ihr:leaving:sync")
    public Result<?> syncSingleQywx(@PathVariable String employeeId) {
        log.info("手动同步单个离职员工到企微, employeeId={}", employeeId);
        try {
            String error = qywxEmployeeLeaveSyncTask.syncSingle(employeeId);
            if (error != null) {
                return Result.success(error);
            }
            return Result.success("同步成功");
        } catch (Exception e) {
            log.error("单个离职员工同步异常, employeeId={}", employeeId, e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }
}
