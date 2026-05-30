package com.bcsport.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bcsport.admin.entity.SysConfig;
import com.bcsport.admin.mapper.SysConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置缓存服务 — 从数据库加载配置到内存，供其他模块快速读取
 */
@Slf4j
@Service
public class ConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        try {
            cache.clear();
            sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                    .select(SysConfig::getConfigKey, SysConfig::getConfigValue))
                    .forEach(c -> cache.put(c.getConfigKey(), c.getConfigValue()));
            log.info("[Config] 配置缓存已加载, 项数={}", cache.size());
        } catch (Exception e) {
            log.warn("[Config] 配置缓存加载失败(表可能未创建), 使用默认值: {}", e.getMessage());
        }
    }

    public String getString(String key, String defaultValue) {
        String val = cache.get(key);
        return val != null ? val : defaultValue;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public int getInt(String key, int defaultValue) {
        String val = cache.get(key);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = cache.get(key);
        if (val == null || val.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(val.trim());
    }
}
