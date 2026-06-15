package com.bcsport.admin.controller.agent;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.agent.PrintTask;
import com.bcsport.admin.service.agent.PrintTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/print")
@Api(tags = "打印任务")
public class PrintController {

    @Autowired
    private PrintTaskService printTaskService;

    @GetMapping("/pull")
    @ApiOperation("Agent 拉取打印任务")
    public Result<List<PrintTask>> pull(@RequestParam String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return Result.paramError("agentId 不能为空");
        }
        List<PrintTask> tasks = printTaskService.pullTasks(agentId);
        return Result.success(tasks);
    }

    @PostMapping("/result")
    @ApiOperation("Agent 回报打印结果")
    public Result<?> reportResult(@RequestBody Map<String, Object> body) {
        String taskId = (String) body.get("taskId");
        Boolean success = (Boolean) body.get("success");
        String message = (String) body.get("message");

        if (taskId == null || taskId.isBlank()) {
            return Result.paramError("taskId 不能为空");
        }
        if (success == null) {
            return Result.paramError("success 不能为空");
        }

        printTaskService.reportResult(taskId, success, message != null ? message : "");
        return Result.success("结果已记录");
    }

    @PostMapping("/create-tasks/{orderId}")
    @ApiOperation("根据申请单创建打印任务")
    public Result<?> createTasks(@PathVariable String orderId, @RequestParam String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return Result.paramError("agentId 不能为空");
        }
        String taskIds = printTaskService.createTasksFromOrder(orderId, agentId);
        return Result.success(taskIds);
    }

    @GetMapping("/tasks/{orderId}")
    @ApiOperation("查询申请单的打印任务")
    public Result<List<PrintTask>> getTasks(@PathVariable String orderId) {
        return Result.success(printTaskService.getTasksByOrderId(orderId));
    }
}
