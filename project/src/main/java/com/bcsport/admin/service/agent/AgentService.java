package com.bcsport.admin.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public void register(String agentId, String agentName, String printers) {
        PrintAgent existing = agentMapper.selectOne(
            new LambdaQueryWrapper<PrintAgent>()
                .eq(PrintAgent::getAgentId, agentId)
        );

        if (existing != null) {
            existing.setAgentName(agentName);
            existing.setPrinters(printers);
            existing.setStatus(1);
            existing.setLastHeartbeat(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            agentMapper.updateById(existing);
        } else {
            PrintAgent agent = new PrintAgent();
            agent.setAgentId(agentId);
            agent.setAgentName(agentName);
            agent.setPrinters(printers);
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

    public List<PrintAgent> listAll() {
        return agentMapper.selectList(
            new LambdaQueryWrapper<PrintAgent>()
                .orderByDesc(PrintAgent::getLastHeartbeat)
        );
    }

    public PrintAgent getByAgentId(String agentId) {
        return agentMapper.selectOne(
            new LambdaQueryWrapper<PrintAgent>()
                .eq(PrintAgent::getAgentId, agentId)
        );
    }

    public void checkOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
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
}
