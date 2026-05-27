package com.bcsport.admin.controller.nxcrm;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.nxcrm.NxcrmMemberTagRecordQueryDTO;
import com.bcsport.admin.dto.nxcrm.NxcrmTagTaskDetailQueryDTO;
import com.bcsport.admin.dto.nxcrm.NxcrmTagTaskQueryDTO;
import com.bcsport.admin.entity.nxcrm.NxcrmTagTask;
import com.bcsport.admin.service.nxcrm.NxcrmTagTaskService;
import com.bcsport.admin.task.nxcrm.NxcrmTagCategorySyncTask;
import com.bcsport.admin.vo.NxcrmMemberTagVO;
import com.bcsport.admin.vo.NxcrmTagTaskDetailVO;
import com.bcsport.admin.vo.NxcrmTagTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/nxcrm/tag")
@Api(tags = "南讯CRM标签任务")
public class NxcrmTagController {

    @Autowired
    private NxcrmTagTaskService tagTaskService;

    @Autowired
    private NxcrmTagCategorySyncTask nxcrmTagCategorySyncTask;

    @GetMapping("/tasks")
    @ApiOperation("分页查询标签任务")
    @RequiresPermissions("nxcrm:tag:query")
    public Result<PageResult<NxcrmTagTaskVO>> getTaskList(PageQuery pageQuery, NxcrmTagTaskQueryDTO queryDTO) {
        return Result.success(tagTaskService.pageTasks(pageQuery, queryDTO));
    }

    @GetMapping("/tasks/{taskId}")
    @ApiOperation("查询任务详情")
    @RequiresPermissions("nxcrm:tag:query")
    public Result<NxcrmTagTaskVO> getTask(@PathVariable String taskId) {
        return Result.success(tagTaskService.getTaskById(taskId));
    }

    @GetMapping("/tasks/{taskId}/details")
    @ApiOperation("查询任务执行明细")
    @RequiresPermissions("nxcrm:tag:query")
    public Result<PageResult<NxcrmTagTaskDetailVO>> getTaskDetails(
            @PathVariable String taskId, PageQuery pageQuery, NxcrmTagTaskDetailQueryDTO queryDTO) {
        return Result.success(tagTaskService.pageTaskDetails(taskId, pageQuery, queryDTO));
    }

    @PostMapping("/tasks")
    @ApiOperation("创建标签任务")
    @RequiresPermissions("nxcrm:tag:add")
    public Result<?> createTask(@RequestBody NxcrmTagTask task) {
        return Result.success(tagTaskService.createTask(task));
    }

    @PostMapping("/tasks/{taskId}/execute")
    @ApiOperation("执行标签任务")
    @RequiresPermissions("nxcrm:tag:execute")
    public Result<?> executeTask(@PathVariable String taskId) {
        return tagTaskService.executeTask(taskId);
    }

    @GetMapping("/tasks/{taskId}/status")
    @ApiOperation("查询任务执行状态")
    @RequiresPermissions("nxcrm:tag:query")
    public Result<?> getTaskStatus(@PathVariable String taskId) {
        return tagTaskService.getTaskStatus(taskId);
    }

    @GetMapping("/member-tags")
    @ApiOperation("查询会员标签记录")
    @RequiresPermissions("nxcrm:tag:query")
    public Result<PageResult<NxcrmMemberTagVO>> getMemberTags(PageQuery pageQuery, NxcrmMemberTagRecordQueryDTO queryDTO) {
        return Result.success(tagTaskService.pageMemberTags(pageQuery, queryDTO));
    }

    @PostMapping("/member-tags/fill-shop")
    @ApiOperation("填充会员店铺信息")
    @RequiresPermissions("nxcrm:tag:execute")
    public Result<?> fillShopId(@RequestParam String batchNo) {
        tagTaskService.fillShopId(batchNo);
        return Result.success("shopId填充完成");
    }

    @GetMapping("/tag-categories")
    @ApiOperation("同步并获取标签分类")
    @RequiresPermissions("nxcrm:tag:query")
    public Result<?> getTagCategories() {
        return Result.success(nxcrmTagCategorySyncTask.syncTagCategories());
    }
}
