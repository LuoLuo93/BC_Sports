package com.bcsport.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ChannelTypeDTO;
import com.bcsport.admin.dto.ChannelTypeQueryDTO;
import com.bcsport.admin.service.ChannelTypeService;
import com.bcsport.admin.vo.ChannelTypeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 渠道类型管理控制器
 */
@RestController
@RequestMapping("/api/channel-type")
@Api(tags = "渠道类型管理")
public class ChannelTypeController {

    @Autowired
    private ChannelTypeService channelTypeService;

    /**
     * 树状查询渠道类型列表
     */
    @GetMapping("/tree")
    @ApiOperation("树状查询渠道类型列表")
    @RequiresPermissions("bi:channelType:query")
    public Result<List<ChannelTypeVO>> listTree(ChannelTypeQueryDTO query) {
        List<ChannelTypeVO> tree = channelTypeService.listByTree(query);
        return Result.success(tree);
    }

    /**
     * 分页查询渠道类型列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询渠道类型列表")
    @RequiresPermissions("bi:channelType:query")
    public Result<IPage<ChannelTypeVO>> listPage(ChannelTypeQueryDTO query) {
        IPage<ChannelTypeVO> page = channelTypeService.listByPage(query);
        return Result.success(page);
    }

    /**
     * 根据ID查询渠道类型
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询渠道类型")
    @RequiresPermissions("bi:channelType:query")
    public Result<ChannelTypeVO> getById(@PathVariable String id) {
        ChannelTypeVO channelType = channelTypeService.getChannelTypeVOById(id);
        if (channelType == null) {
            return Result.notFound("渠道类型不存在");
        }
        return Result.success(channelType);
    }

    /**
     * 新增渠道类型
     */
    @PostMapping
    @ApiOperation("新增渠道类型")
    @RequiresPermissions("bi:channelType:add")
    public Result<String> add(@Valid @RequestBody ChannelTypeDTO channelTypeDTO) {
        channelTypeService.addChannelType(channelTypeDTO);
        return Result.success("渠道类型添加成功");
    }

    /**
     * 修改渠道类型
     */
    @PutMapping("/{id}")
    @ApiOperation("修改渠道类型")
    @RequiresPermissions("bi:channelType:edit")
    public Result<String> update(@PathVariable String id, @Valid @RequestBody ChannelTypeDTO channelTypeDTO) {
        channelTypeDTO.setId(id);
        channelTypeService.updateChannelType(channelTypeDTO);
        return Result.success("渠道类型更新成功");
    }

    /**
     * 删除渠道类型
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除渠道类型")
    @RequiresPermissions("bi:channelType:delete")
    public Result<String> delete(@PathVariable String id) {
        channelTypeService.deleteChannelType(id);
        return Result.success("渠道类型已成功删除");
    }
}
