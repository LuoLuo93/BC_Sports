package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.SysConfig;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.SysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/config")
@Api(tags = "系统配置")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ConfigService configService;

    @Value("${bc.upload.path:E:/work/BC_Sport/BcSportsDataManageSystem/project/src/main/resources/static/images}")
    private String uploadBasePath;

    /**
     * 公开配置（无需登录，登录页使用）
     */
    @GetMapping("/public")
    @ApiOperation("获取公开配置")
    public Result<Map<String, String>> getPublic() {
        Map<String, String> map = new HashMap<>();
        map.put("sys.name", configService.getString("sys.name", "BC体育数据管理系统"));
        map.put("sys.pageSize", configService.getString("sys.pageSize", "20"));
        map.put("sys.dateFormat", configService.getString("sys.dateFormat", "yyyy-MM-dd HH:mm:ss"));
        map.put("sys.timezone", configService.getString("sys.timezone", "GMT+8"));
        map.put("security.captchaEnabled", String.valueOf(configService.getBoolean("security.captchaEnabled", true)));
        map.put("sys.logoUrl", configService.getString("sys.logoUrl", ""));
        return Result.success(map);
    }

    @GetMapping
    @RequiresPermissions("system:config:query")
    @ApiOperation("获取所有配置")
    public Result<List<SysConfig>> getAll() {
        List<SysConfig> list = sysConfigService.getAllConfigs();
        return Result.success(list);
    }

    @PutMapping
    @RequiresPermissions("system:config:edit")
    @OperLog(module = "系统设置", operation = "修改配置")
    @ApiOperation("批量更新配置")
    public Result<Void> update(@RequestBody Map<String, String> configs) {
        sysConfigService.updateConfigs(configs);
        return Result.success();
    }

    @PostMapping("/upload-logo")
    @RequiresPermissions("system:config:edit")
    @OperLog(module = "系统设置", operation = "上传Logo")
    @ApiOperation("上传系统Logo")
    public Result<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请选择图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.paramError("仅支持图片文件");
        }

        try {
            String ext = "png";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf(".") + 1).toLowerCase();
            }
            String filename = "logo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;

            Path uploadDir = Paths.get(uploadBasePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path target = uploadDir.resolve(filename);
            file.transferTo(target.toFile());

            String url = "/images/" + filename;
            log.info("[Config] Logo已上传: {}", url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("[Config] Logo上传失败", e);
            return Result.error("上传失败，请检查文件格式和大小");
        }
    }
}
