package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.QywxMomentQueryDTO;
import com.bcsport.admin.qywxmapper.QywxMomentMapper;
import com.bcsport.admin.task.qywx.QywxMomentTask;
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
@RequestMapping("/api/qywx/moment")
@Api(tags = "企微朋友圈管理")
public class QywxMomentController {

    @Autowired
    private QywxMomentMapper momentMapper;

    @Autowired
    private QywxMomentTask momentTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/page")
    @ApiOperation("分页查询朋友圈列表")
    @RequiresPermissions("qywx:moment:query")
    public Result<Map<String, Object>> page(@Valid PageQuery pageQuery,
                                            QywxMomentQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = momentMapper.selectPage(queryDTO.getCreatorName(), offset, pageSize);
        long total = momentMapper.selectCount(queryDTO.getCreatorName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @PostMapping("/sync")
    @ApiOperation("同步朋友圈数据")
    @RequiresPermissions("qywx:moment:query")
    public Result<String> sync() {
        if (QywxMomentTask.isSyncing()) {
            return Result.error("同步任务正在进行中，请稍后再试");
        }
        taskThreadPool.execute(() -> {
            try {
                momentTask.sync();
            } catch (Exception e) {
                log.error("同步朋友圈数据异常", e);
            }
        });
        return Result.success("同步任务已触发");
    }

    @GetMapping("/sync-status")
    @ApiOperation("同步状态")
    @RequiresPermissions("qywx:moment:query")
    public Result<Map<String, Object>> syncStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("syncing", QywxMomentTask.isSyncing());
        return Result.success(data);
    }
}
