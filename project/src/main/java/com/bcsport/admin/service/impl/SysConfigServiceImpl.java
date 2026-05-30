package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.entity.SysConfig;
import com.bcsport.admin.mapper.SysConfigMapper;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.SysConfigService;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private DefaultWebSessionManager sessionManager;

    @Override
    public List<SysConfig> getAllConfigs() {
        return list(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigGroup, SysConfig::getSort));
    }

    @Override
    public List<SysConfig> getConfigsByGroup(String group) {
        return list(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigGroup, group)
                .orderByAsc(SysConfig::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, entry.getKey());
            SysConfig existing = getOne(wrapper);
            if (existing != null) {
                existing.setConfigValue(entry.getValue());
                updateById(existing);
            }
        }
        configService.reload();

        // 动态更新会话超时
        if (configs.containsKey("security.sessionTimeout")) {
            int timeoutMinutes = configService.getInt("security.sessionTimeout", 30);
            sessionManager.setGlobalSessionTimeout(timeoutMinutes * 60 * 1000L);
            log.info("[Config] 会话超时已更新为 {} 分钟", timeoutMinutes);
        }

        log.info("[Config] 系统配置已更新, 项数={}", configs.size());
    }
}
