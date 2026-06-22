package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.QywxCustomerQueryDTO;
import com.bcsport.admin.dto.QywxFollowUserQueryDTO;
import com.bcsport.admin.dto.QywxGroupStatQueryDTO;
import com.bcsport.admin.qywxmapper.QywxFollowUserMapper;
import com.bcsport.admin.qywxmapper.QywxMomentMapper;
import com.bcsport.admin.qywxmapper.VxCustomerBaseDetailsMapper;
import com.bcsport.admin.qywxmapper.VxCustomerlistdetailsFollowInfoMapper;
import com.bcsport.admin.qywxmapper.VxGroupchatYesterdayMapper;
import com.bcsport.admin.task.qywx.QywxFollowUserTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ThreadPoolExecutor;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/qywx/follow-user")
@Api(tags = "企微客户联系成员管理")
public class QywxFollowUserController {

    @Autowired
    private QywxFollowUserMapper followUserMapper;

    @Autowired
    private VxCustomerlistdetailsFollowInfoMapper followInfoMapper;

    @Autowired
    private VxGroupchatYesterdayMapper groupchatYesterdayMapper;

    @Autowired
    private VxCustomerBaseDetailsMapper customerBaseDetailsMapper;

    @Autowired
    private QywxMomentMapper momentMapper;

    @Autowired
    private QywxFollowUserTask followUserTask;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/page")
    @ApiOperation("分页查询客户联系成员列表")
    @RequiresPermissions("qywx:follow:query")
    public Result<Map<String, Object>> page(@Valid PageQuery pageQuery,
                                            QywxFollowUserQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = followUserMapper.selectPageWithDetail(queryDTO.getName(), queryDTO.getMobile(), queryDTO.getMainDepartment(), offset, pageSize);
        long total = followUserMapper.selectCountWithDetail(queryDTO.getName(), queryDTO.getMobile(), queryDTO.getMainDepartment());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @GetMapping("/customers")
    @ApiOperation("查询某成员跟进的客户列表")
    @RequiresPermissions("qywx:follow:query")
    public Result<Map<String, Object>> customers(@Valid PageQuery pageQuery,
                                                  QywxCustomerQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = followInfoMapper.selectCustomerPageByUserid(queryDTO.getUserid(), queryDTO.getName(), offset, pageSize);
        long total = followInfoMapper.selectCustomerCountByUserid(queryDTO.getUserid(), queryDTO.getName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @GetMapping("/group-chats")
    @ApiOperation("查询某成员的群聊列表")
    @RequiresPermissions("qywx:follow:query")
    public Result<Map<String, Object>> groupChats(@Valid PageQuery pageQuery,
                                                   QywxCustomerQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = customerBaseDetailsMapper.selectPageByOwner(queryDTO.getUserid(), queryDTO.getName(), offset, pageSize);
        long total = customerBaseDetailsMapper.selectCountByOwner(queryDTO.getUserid(), queryDTO.getName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @GetMapping("/moments")
    @ApiOperation("查询某成员的朋友圈")
    @RequiresPermissions("qywx:follow:query")
    public Result<Map<String, Object>> moments(@Valid PageQuery pageQuery,
                                                QywxCustomerQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = momentMapper.selectPageByCreator(queryDTO.getUserid(), offset, pageSize);
        long total = momentMapper.selectCountByCreator(queryDTO.getUserid());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @GetMapping("/group-stats")
    @ApiOperation("查询某成员的群聊统计")
    @RequiresPermissions("qywx:follow:query")
    public Result<Map<String, Object>> groupStats(@Valid PageQuery pageQuery,
                                                   QywxGroupStatQueryDTO queryDTO) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 100), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<Map<String, Object>> records = groupchatYesterdayMapper.selectPageByOwner(queryDTO.getOwner(), offset, pageSize);
        long total = groupchatYesterdayMapper.selectCountByOwner(queryDTO.getOwner());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @PostMapping("/sync")
    @ApiOperation("同步客户联系成员")
    @RequiresPermissions("qywx:follow:query")
    public Result<String> sync() {
        taskThreadPool.execute(() -> {
            try {
                followUserTask.sync();
            } catch (Exception e) {
                log.error("同步客户联系成员异常", e);
            }
        });
        return Result.success("同步任务已触发，请稍后刷新页面查看数据");
    }
}
