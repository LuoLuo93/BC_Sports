package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.importer.LegacyImportLogMigrator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统一导入日志 —— 运维入口。
 * 各模块的日志分页查询仍走各自 /import-log/page 接口并沿用各自权限，这里不提供跨模块查询。
 */
@Slf4j
@RestController
@RequestMapping("/api/import-log")
@Api(tags = "统一导入日志")
public class ImportLogController {

    @Autowired
    private LegacyImportLogMigrator legacyImportLogMigrator;

    @PostMapping("/migrate-legacy")
    @ApiOperation("一次性搬迁各模块旧导入日志到统一表（幂等，可重复执行；R01 阶段 4 清理时删除）")
    @RequiresPermissions("system:import-log:migrate")
    public Result<Map<String, Object>> migrateLegacy() {
        Map<String, Object> report = legacyImportLogMigrator.migrateAll();
        log.info("旧导入日志搬迁完成: {}", report);
        return Result.success(report);
    }
}
