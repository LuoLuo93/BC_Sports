package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.SysLogQueryDTO;
import com.bcsport.admin.entity.SysLog;
import com.bcsport.admin.service.SysLogService;
import com.bcsport.admin.vo.ModuleTreeNode;
import lombok.extern.slf4j.Slf4j;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/log")
@Api(tags = "操作日志")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    @GetMapping("/page")
    @ApiOperation("分页查询操作日志")
    @RequiresPermissions("system:log:query")
    public Result<PageResult<SysLog>> pageLogs(PageQuery pageQuery, SysLogQueryDTO queryDTO) {
        return Result.success(sysLogService.pageLogs(pageQuery, queryDTO));
    }

    @GetMapping("/modules")
    @ApiOperation("日志模块按菜单目录分组后的两级树，供筛选下拉动态加载")
    @RequiresPermissions("system:log:query")
    public Result<List<ModuleTreeNode>> listModuleTree() {
        return Result.success(sysLogService.listModuleTree());
    }

    @DeleteMapping("/clean")
    @ApiOperation("清理操作日志")
    @OperLog(module = "系统维护", operation = "清理操作日志")
    @RequiresPermissions("system:log:remove")
    public Result<Integer> cleanLogs(@RequestParam(defaultValue = "30") int days) {
        int count = sysLogService.cleanLogs(days);
        return Result.success("清理完成，共删除" + count + "条记录", count);
    }
}
