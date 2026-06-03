package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.service.AuthCacheService;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.SysLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AuthCacheService authCacheService;

    /**
     * 清除系统缓存（Redis + 内存配置缓存）
     */
    @PostMapping("/clear-cache")
    @RequiresPermissions("system:config:edit")
    public Result<Void> clearCache() {
        try {
            // 选择性清除 Redis 中的权限/角色/菜单缓存（保留会话和登录状态）
            authCacheService.evictAll();

            // 重新加载内存配置缓存
            configService.reload();

            log.info("[维护] 系统缓存已清除并重新加载");
            return Result.success("缓存已清除", null);
        } catch (Exception e) {
            log.error("[维护] 清除缓存失败", e);
            return Result.error("清除缓存失败，请稍后重试");
        }
    }

    /**
     * 清理操作日志
     */
    @PostMapping("/clean-logs")
    @RequiresPermissions("system:log:remove")
    public Result<Integer> cleanLogs(@RequestParam(defaultValue = "30") int days) {
        int count = sysLogService.cleanLogs(days);
        log.info("[维护] 已清理 {} 天前的操作日志, 删除 {} 条", days, count);
        return Result.success("清理完成，共删除 " + count + " 条记录", count);
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    @RequiresPermissions("system:config:query")
    public Result<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();

        // Redis 状态
        try {
            redisTemplate.opsForValue().set("__health_check__", "ok");
            String val = (String) redisTemplate.opsForValue().get("__health_check__");
            redisTemplate.delete("__health_check__");
            info.put("redis", "ok".equals(val) ? "正常" : "异常");
        } catch (Exception e) {
            info.put("redis", "连接失败: " + e.getMessage());
        }

        // 数据库状态
        try {
            var factory = redisTemplate.getConnectionFactory();
            // 通过 configService 是否能正常加载来间接判断 DB
            configService.reload();
            info.put("database", "正常");
        } catch (Exception e) {
            info.put("database", "连接失败: " + e.getMessage());
        }

        // 磁盘空间
        try {
            File root = new File("/");
            if (!root.exists()) root = new File("C:\\");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long used = total - free;
            int usedPercent = (int) (used * 100 / total);
            info.put("diskTotal", formatSize(total));
            info.put("diskUsed", formatSize(used));
            info.put("diskFree", formatSize(free));
            info.put("diskUsedPercent", usedPercent + "%");
        } catch (Exception e) {
            info.put("disk", "获取失败");
        }

        // JVM 内存
        Runtime rt = Runtime.getRuntime();
        long jvmTotal = rt.totalMemory();
        long jvmFree = rt.freeMemory();
        long jvmUsed = jvmTotal - jvmFree;
        info.put("jvmTotal", formatSize(jvmTotal));
        info.put("jvmUsed", formatSize(jvmUsed));
        info.put("jvmFree", formatSize(jvmFree));

        // 配置项数量
        info.put("configCount", configService.getString("sys.name") != null ? "正常加载" : "未加载");

        return Result.success(info);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
