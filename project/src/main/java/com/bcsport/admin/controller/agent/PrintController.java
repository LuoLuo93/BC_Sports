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
        // 语义状态(可选)：completed/paused/failed。传了优先用；没传则按老协议 success 布尔值判断
        String resultStatus = body.get("status") == null ? null : body.get("status").toString();

        if (taskId == null || taskId.isBlank()) {
            return Result.paramError("taskId 不能为空");
        }
        // 向后兼容：没传 status 时，success 必填
        if ((resultStatus == null || resultStatus.isBlank()) && success == null) {
            return Result.paramError("success 不能为空");
        }

        printTaskService.reportResult(taskId, resultStatus, success, message != null ? message : "");
        return Result.success("结果已记录");
    }

    @PostMapping("/create-tasks/{orderId}")
    @ApiOperation("根据申请单创建打印任务")
    public Result<?> createTasks(@PathVariable String orderId, @RequestParam String agentId,
                                 @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        if (agentId == null || agentId.isBlank()) {
            return Result.paramError("agentId 不能为空");
        }
        String taskIds = printTaskService.createTasksFromOrder(orderId, agentId, force);
        return Result.success(taskIds);
    }

    @GetMapping("/tasks/{orderId}")
    @ApiOperation("查询申请单的打印任务")
    public Result<List<PrintTask>> getTasks(@PathVariable String orderId) {
        return Result.success(printTaskService.getTasksByOrderId(orderId));
    }

    @GetMapping("/tasks/{orderId}/pending-summary")
    @ApiOperation("统计申请单未完成任务数(待打印/打印中/已暂停)——下发前轻量预检查用")
    public Result<Map<String, Long>> pendingSummary(@PathVariable String orderId) {
        return Result.success(printTaskService.countUnfinishedByOrderId(orderId));
    }

    @PostMapping("/cancel")
    @ApiOperation("手动取消单个打印任务(仅待打印/打印中/已暂停)")
    public Result<?> cancelTask(@RequestBody Map<String, Object> body) {
        String taskId = (String) body.get("taskId");
        String reason = body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
        if (taskId == null || taskId.isBlank()) {
            return Result.paramError("taskId 不能为空");
        }
        printTaskService.cancelTask(taskId, reason);
        return Result.success("任务已取消");
    }

    @PostMapping("/reprint")
    @ApiOperation("补打单个打印任务")
    public Result<?> reprint(@RequestBody Map<String, Object> body) {
        String taskId = (String) body.get("taskId");
        String agentId = (String) body.get("agentId");
        String reason = body.get("reason") != null ? String.valueOf(body.get("reason")) : null;

        if (taskId == null || taskId.isBlank()) {
            return Result.paramError("taskId 不能为空");
        }
        if (agentId == null || agentId.isBlank()) {
            return Result.paramError("agentId 不能为空");
        }

        String newTaskId = printTaskService.reprintTask(taskId, agentId, reason);
        return Result.success(newTaskId);
    }
}
