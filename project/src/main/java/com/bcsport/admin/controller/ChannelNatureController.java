package com.bcsport.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ChannelNatureDTO;
import com.bcsport.admin.dto.ChannelNatureQueryDTO;
import com.bcsport.admin.service.ChannelNatureService;
import com.bcsport.admin.vo.ChannelNatureVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 渠道性质管理控制器
 */
@RestController
@RequestMapping("/api/channel-nature")
@Api(tags = "渠道性质管理")
public class ChannelNatureController {

    @Autowired
    private ChannelNatureService channelNatureService;

    /**
     * 树状查询渠道性质列表
     */
    @GetMapping("/tree")
    @ApiOperation("树状查询渠道性质列表")
    @RequiresPermissions("bi:channelNature:query")
    public Result<List<ChannelNatureVO>> listTree(ChannelNatureQueryDTO query) {
        List<ChannelNatureVO> tree = channelNatureService.listByTree(query);
        return Result.success(tree);
    }

    /**
     * 分页查询渠道性质列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询渠道性质列表")
    @RequiresPermissions("bi:channelNature:query")
    public Result<IPage<ChannelNatureVO>> listPage(ChannelNatureQueryDTO query) {
        IPage<ChannelNatureVO> page = channelNatureService.listByPage(query);
        return Result.success(page);
    }

    /**
     * 根据ID查询渠道性质
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询渠道性质")
    @RequiresPermissions("bi:channelNature:query")
    public Result<ChannelNatureVO> getById(@PathVariable String id) {
        ChannelNatureVO channelNature = channelNatureService.getChannelNatureVOById(id);
        if (channelNature == null) {
            return Result.notFound("渠道性质不存在");
        }
        return Result.success(channelNature);
    }

    /**
     * 新增渠道性质
     */
    @PostMapping
    @ApiOperation("新增渠道性质")
    @RequiresPermissions("bi:channelNature:add")
    public Result<String> add(@Valid @RequestBody ChannelNatureDTO channelNatureDTO) {
        channelNatureService.addChannelNature(channelNatureDTO);
        return Result.success("渠道性质添加成功");
    }

    /**
     * 修改渠道性质
     */
    @PutMapping("/{id}")
    @ApiOperation("修改渠道性质")
    @RequiresPermissions("bi:channelNature:edit")
    public Result<String> update(@PathVariable String id, @Valid @RequestBody ChannelNatureDTO channelNatureDTO) {
        channelNatureDTO.setId(id);
        channelNatureService.updateChannelNature(channelNatureDTO);
        return Result.success("渠道性质更新成功");
    }

    /**
     * 删除渠道性质
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除渠道性质")
    @RequiresPermissions("bi:channelNature:delete")
    public Result<String> delete(@PathVariable String id) {
        channelNatureService.deleteChannelNature(id);
        return Result.success("渠道性质已成功删除");
    }
}
