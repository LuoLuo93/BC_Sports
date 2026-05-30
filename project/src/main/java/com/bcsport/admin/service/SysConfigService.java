package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.entity.SysConfig;

import java.util.List;
import java.util.Map;

public interface SysConfigService extends IService<SysConfig> {

    List<SysConfig> getAllConfigs();

    List<SysConfig> getConfigsByGroup(String group);

    void updateConfigs(Map<String, String> configs);
}
