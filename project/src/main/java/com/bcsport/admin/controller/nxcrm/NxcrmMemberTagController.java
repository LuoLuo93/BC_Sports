package com.bcsport.admin.controller.nxcrm;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.nxcrm.NxcrmMemberTagDetailQueryDTO;
import com.bcsport.admin.dto.nxcrm.NxcrmTagListQueryDTO;
import com.bcsport.admin.service.nxcrm.NxcrmMemberTagService;
import com.bcsport.admin.task.nxcrm.NxcrmMemberTagPushTask;
import com.bcsport.admin.task.nxcrm.NxcrmTagIncrementSyncTask;
import com.bcsport.admin.vo.NxcrmMemberTagDetailVO;
import com.bcsport.admin.vo.NxcrmTagInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/api/nxcrm/member-tag-mgmt")
@Api(tags = "南讯会员标签管理")
public class NxcrmMemberTagController {

    @Autowired
    private NxcrmMemberTagService memberTagService;

    @Autowired
    private NxcrmTagIncrementSyncTask tagIncrementSyncTask;

    @Autowired
    private NxcrmMemberTagPushTask memberTagPushTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/member-detail")
    @ApiOperation("查询会员标签详情")
    @RequiresPermissions("nxcrm:member:tag:query")
    public Result<PageResult<NxcrmMemberTagDetailVO>> memberDetail(PageQuery pageQuery, NxcrmMemberTagDetailQueryDTO queryDTO) {
        return Result.success(memberTagService.pageMemberTags(pageQuery, queryDTO));
    }

    @GetMapping("/tag-list")
    @ApiOperation("查询南讯标签列表")
    @RequiresPermissions("nxcrm:member:tag:query")
    public Result<PageResult<NxcrmTagInfoVO>> tagList(PageQuery pageQuery, NxcrmTagListQueryDTO queryDTO) {
        return Result.success(memberTagService.pageTagList(pageQuery, queryDTO));
    }

    @PostMapping("/sync-tags")
    @ApiOperation("同步标签数据")
    @RequiresPermissions("nxcrm:member:tag:sync")
    public Result<?> syncTags() {
        if (NxcrmTagIncrementSyncTask.isSyncing()) {
            return Result.error("标签数据同步正在进行中");
        }
        taskThreadPool.submit(() -> tagIncrementSyncTask.syncIncrementTags(null));
        return Result.success("标签数据同步已触发");
    }

    @PostMapping("/sync-member-tags")
    @ApiOperation("同步会员标签")
    @RequiresPermissions("nxcrm:member:tag:sync")
    public Result<?> syncMemberTags() {
        if (NxcrmMemberTagPushTask.isSyncing()) {
            return Result.error("会员标签同步正在进行中");
        }
        taskThreadPool.submit(() -> memberTagPushTask.pushMemberTags(null));
        return Result.success("会员标签同步已触发");
    }

    @GetMapping("/sync-status")
    @ApiOperation("查询同步状态")
    @RequiresPermissions("nxcrm:member:tag:query")
    public Result<Map<String, Object>> getSyncStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tagSyncing", NxcrmTagIncrementSyncTask.isSyncing());
        data.put("memberTagSyncing", NxcrmMemberTagPushTask.isSyncing());
        return Result.success(data);
    }
}
