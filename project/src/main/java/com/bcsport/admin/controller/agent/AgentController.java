package com.bcsport.admin.controller.agent;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.agent.PrintAgent;
import com.bcsport.admin.entity.agent.PrintTask;
import com.bcsport.admin.service.agent.AgentService;
import com.bcsport.admin.service.agent.PrintTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@Api(tags = "Agent 管理")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private PrintTaskService printTaskService;

    @PostMapping("/register")
    @ApiOperation("Agent 注册")
    public Result<?> register(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String agentId = (String) body.get("agentId");
        String agentName = (String) body.get("agentName");
        List<String> printers = (List<String>) body.get("printers");

        if (agentId == null || agentId.isBlank()) {
            return Result.paramError("agentId 不能为空");
        }

        String printersStr = printers != null ? String.join(",", printers) : "";
        String ipAddress = request.getRemoteAddr();
        agentService.register(agentId, agentName, printersStr, ipAddress);
        return Result.success("注册成功");
    }

    @PostMapping("/heartbeat")
    @ApiOperation("Agent 心跳")
    public Result<?> heartbeat(@RequestBody Map<String, String> body) {
        String agentId = body.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            return Result.paramError("agentId 不能为空");
        }

        agentService.heartbeat(agentId);
        return Result.success("OK");
    }

    @GetMapping("/list")
    @ApiOperation("Agent 列表")
    public Result<List<PrintAgent>> list() {
        return Result.success(agentService.listAll());
    }

    @GetMapping("/page")
    @ApiOperation("分页查询 Agent")
    public Result<PageResult<PrintAgent>> page(@Valid PageQuery pageQuery,
                                               @RequestParam(required = false) String agentName) {
        var page = agentService.page(pageQuery.getPageNum(), pageQuery.getPageSize(), agentName);
        return Result.success(PageResult.of(page));
    }

    @GetMapping("/{agentId}")
    @ApiOperation("Agent 详情")
    public Result<PrintAgent> getDetail(@PathVariable String agentId) {
        PrintAgent agent = agentService.getByAgentId(agentId);
        if (agent == null) {
            return Result.error("Agent 不存在");
        }
        return Result.success(agent);
    }

    @GetMapping("/{agentId}/tasks")
    @ApiOperation("Agent 任务记录")
    public Result<List<PrintTask>> getTasks(@PathVariable String agentId) {
        return Result.success(printTaskService.getTasksByAgentId(agentId));
    }

    @GetMapping("/{agentId}/tasks/page")
    @ApiOperation("Agent 任务记录（分页）")
    public Result<PageResult<PrintTask>> getTasksPage(@Valid PageQuery pageQuery,
                                                      @PathVariable String agentId) {
        return Result.success(PageResult.of(printTaskService.getTasksByAgentIdPage(pageQuery.getPageNum(), pageQuery.getPageSize(), agentId)));
    }
}
