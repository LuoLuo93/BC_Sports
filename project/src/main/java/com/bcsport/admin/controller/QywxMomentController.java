package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.QywxMomentQueryDTO;
import com.bcsport.admin.qywxmapper.QywxMomentMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/qywx/moment")
@Api(tags = "企微朋友圈管理")
public class QywxMomentController {

    @Autowired
    private QywxMomentMapper momentMapper;

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
}
