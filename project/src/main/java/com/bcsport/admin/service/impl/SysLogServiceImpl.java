package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SysLogQueryDTO;
import com.bcsport.admin.entity.SysLog;
import com.bcsport.admin.mapper.SysLogMapper;
import com.bcsport.admin.service.SysLogService;
import com.bcsport.admin.vo.ModuleTreeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysLogServiceImpl implements SysLogService {

    @Autowired
    private SysLogMapper sysLogMapper;

    @Override
    public PageResult<SysLog> pageLogs(PageQuery pageQuery, SysLogQueryDTO queryDTO) {
        IPage<SysLog> page = sysLogMapper.selectLogPage(pageQuery.toPage(), queryDTO);
        return new PageResult<>(page);
    }

    @Override
    @Async
    public void saveLog(SysLog sysLog) {
        try {
            log.info("[OperLog] 保存日志: module={}, operation={}, username={}", sysLog.getModule(), sysLog.getOperation(), sysLog.getUsername());
            int rows = sysLogMapper.insert(sysLog);
            log.info("[OperLog] 保存完成: rows={}", rows);
        } catch (Exception e) {
            log.error("[OperLog] 保存操作日志失败: module={}, operation={}, error={}", sysLog.getModule(), sysLog.getOperation(), e.getMessage(), e);
        }
    }

    @Override
    public int cleanLogs(int days) {
        return sysLogMapper.deleteBeforeDays(days);
    }

    /**
     * 模块 -> 菜单目录分组。分组名跟随系统菜单的一级目录命名(见 bc_sports_sys_menu)。
     * 选项本身取自日志表 DISTINCT 结果：这里没收录的新模块会自动落到"其他"组，
     * 不会像旧版写死下拉那样从筛选项里消失。
     */
    private static final Map<String, List<String>> MODULE_GROUPS = new LinkedHashMap<>();

    static {
        MODULE_GROUPS.put("系统管理", List.of("系统认证", "用户管理", "角色管理", "菜单管理", "部门管理",
                "字典管理", "在线用户", "系统设置", "系统维护", "通知管理"));
        MODULE_GROUPS.put("运维监控", List.of("定时任务", "Jenkins监控"));
        MODULE_GROUPS.put("BI管理", List.of("品牌管理", "地区管理", "渠道类型", "渠道性质", "仓库管理",
                "达人店铺", "店铺日预算", "新旧品管理", "首次添加记录", "数仓销售", "实体渠道",
                "ERP店铺管理", "ERP仓库管理", "ERP店仓管理"));
        MODULE_GROUPS.put("ERP管理", List.of("预估成本", "揽众客户"));
        MODULE_GROUPS.put("人事同步", List.of("人事同步", "人事排除名单"));
        MODULE_GROUPS.put("企业微信", List.of("企微标签", "企微客户"));
        MODULE_GROUPS.put("牛信CRM", List.of("牛信CRM"));
        MODULE_GROUPS.put("贴纸打印", List.of("贴纸打印"));
        MODULE_GROUPS.put("BC好玩家", List.of("运动积分"));
    }

    @Override
    public List<ModuleTreeNode> listModuleTree() {
        List<String> modules = sysLogMapper.selectDistinctModules();
        Set<String> known = MODULE_GROUPS.values().stream().flatMap(List::stream)
                .collect(Collectors.toSet());

        List<ModuleTreeNode> tree = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : MODULE_GROUPS.entrySet()) {
            List<ModuleTreeNode> children = entry.getValue().stream()
                    .filter(modules::contains)
                    .map(ModuleTreeNode::leaf)
                    .collect(Collectors.toList());
            if (!children.isEmpty()) {
                tree.add(ModuleTreeNode.group(entry.getKey(), children));
            }
        }
        // 未收录进分组的模块(新增 @OperLog 后尚未登记)统一放"其他"，保证可见可筛
        Set<String> unknown = new LinkedHashSet<>(modules);
        unknown.removeAll(known);
        if (!unknown.isEmpty()) {
            tree.add(ModuleTreeNode.group("其他",
                    unknown.stream().map(ModuleTreeNode::leaf).collect(Collectors.toList())));
        }
        return tree;
    }
}
