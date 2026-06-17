package com.bcsport.admin.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.entity.agent.PrintAgent;
import com.bcsport.admin.mapper.agent.PrintAgentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentService {

    @Autowired
    private PrintAgentMapper agentMapper;

    public void register(String agentId, String agentName, String printers, String ipAddress) {
        PrintAgent existing = agentMapper.selectOne(
            new LambdaQueryWrapper<PrintAgent>()
                .eq(PrintAgent::getAgentId, agentId)
        );

        if (existing != null) {
            existing.setAgentName(agentName);
            existing.setPrinters(printers);
            existing.setIpAddress(ipAddress);
            existing.setStatus(1);
            existing.setLastHeartbeat(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            agentMapper.updateById(existing);
        } else {
            PrintAgent agent = new PrintAgent();
            agent.setAgentId(agentId);
            agent.setAgentName(agentName);
            agent.setPrinters(printers);
            agent.setIpAddress(ipAddress);
            agent.setStatus(1);
            agent.setLastHeartbeat(LocalDateTime.now());
            agent.setCreateTime(LocalDateTime.now());
            agent.setUpdateTime(LocalDateTime.now());
            agentMapper.insert(agent);
        }
    }

    public void heartbeat(String agentId) {
        PrintAgent agent = agentMapper.selectOne(
            new LambdaQueryWrapper<PrintAgent>()
                .eq(PrintAgent::getAgentId, agentId)
        );

        if (agent != null) {
            agent.setLastHeartbeat(LocalDateTime.now());
            agent.setStatus(1);
            agentMapper.updateById(agent);
        }
    }

    /** 心跳超过该秒数即视为离线（与 checkOffline 保持一致，前端"刚刚"阈值为60秒）。 */
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    /**
     * 列出全部 Agent，并按最后心跳实时重算在线/离线状态（仅用于展示，不写库），
     * 避免存储的 status 滞后导致页面显示“假在线”。监控页刷新、下发时选 Agent 均以此为准。
     */
    public List<PrintAgent> listAll() {
        List<PrintAgent> agents = agentMapper.selectList(
            new LambdaQueryWrapper<PrintAgent>()
                .orderByDesc(PrintAgent::getLastHeartbeat)
        );
        agents.forEach(this::applyLiveStatus);
        return agents;
    }

    /** 根据最后心跳实时判定在线状态（心跳在 HEARTBEAT_TIMEOUT_SECONDS 内视为在线）。 */
    private void applyLiveStatus(PrintAgent agent) {
        if (agent == null) return;
        LocalDateTime hb = agent.getLastHeartbeat();
        boolean alive = hb != null && hb.isAfter(LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS));
        agent.setStatus(alive ? 1 : 0);
    }

    public PrintAgent getByAgentId(String agentId) {
        PrintAgent agent = agentMapper.selectOne(
            new LambdaQueryWrapper<PrintAgent>()
                .eq(PrintAgent::getAgentId, agentId)
        );
        applyLiveStatus(agent);
        return agent;
    }

    public void checkOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        List<PrintAgent> agents = agentMapper.selectList(
            new LambdaQueryWrapper<PrintAgent>()
                .eq(PrintAgent::getStatus, 1)
                .lt(PrintAgent::getLastHeartbeat, threshold)
        );

        for (PrintAgent agent : agents) {
            agent.setStatus(0);
            agentMapper.updateById(agent);
        }
    }

    /**
     * 分页查询 Agent 列表。
     * 状态由前端根据 lastHeartbeat 实时计算，后端只返回原始数据。
     */
    public IPage<PrintAgent> page(int pageNum, int pageSize, String agentName) {
        LambdaQueryWrapper<PrintAgent> wrapper = new LambdaQueryWrapper<>();
        if (agentName != null && !agentName.isBlank()) {
            wrapper.like(PrintAgent::getAgentName, agentName);
        }
        wrapper.orderByDesc(PrintAgent::getLastHeartbeat);
        return agentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
