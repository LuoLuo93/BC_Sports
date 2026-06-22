package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.QywxGroupChatQueryDTO;
import com.bcsport.admin.qywxmapper.VxCustomerBaseDetailsMapper;
import com.bcsport.admin.task.qywx.QywxGroupChatTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RestController
@RequestMapping("/api/qywx/group-chat")
@Api(tags = "企微群聊管理")
public class QywxGroupChatController {

    @Autowired
    private VxCustomerBaseDetailsMapper customerBaseDetailsMapper;

    @Autowired
    private QywxGroupChatTask groupChatTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/page")
    @ApiOperation("分页查询群聊列表")
    @RequiresPermissions("qywx:groupchat:query")
    public Result<Map<String, Object>> page(@Valid PageQuery pageQuery,
                                            QywxGroupChatQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = customerBaseDetailsMapper.selectPage(queryDTO.getName(), queryDTO.getOwnerName(), offset, pageSize);
        long total = customerBaseDetailsMapper.selectCount(queryDTO.getName(), queryDTO.getOwnerName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @GetMapping("/members")
    @ApiOperation("查询群成员列表")
    @RequiresPermissions("qywx:groupchat:query")
    public Result<List<Map<String, Object>>> members(@RequestParam String chatId) {
        List<Map<String, Object>> members = customerBaseDetailsMapper.selectMembersByChatId(chatId);
        return Result.success(members);
    }

    @PostMapping("/sync")
    @ApiOperation("同步群聊数据")
    @RequiresPermissions("qywx:groupchat:query")
    public Result<String> sync() {
        if (QywxGroupChatTask.isSyncing()) {
            return Result.error("同步任务正在进行中，请稍后再试");
        }
        taskThreadPool.execute(() -> {
            try {
                groupChatTask.sync();
            } catch (Exception e) {
                log.error("同步群聊数据异常", e);
            }
        });
        return Result.success("同步任务已触发");
    }

    @GetMapping("/sync-status")
    @ApiOperation("同步状态")
    @RequiresPermissions("qywx:groupchat:query")
    public Result<Map<String, Object>> syncStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("syncing", QywxGroupChatTask.isSyncing());
        return Result.success(data);
    }
}
