package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.EntityChannelDTO;
import com.bcsport.admin.dto.EntityChannelQueryDTO;
import com.bcsport.admin.service.EntityChannelService;
import com.bcsport.admin.vo.EntityChannelVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 实体渠道配置控制器
 */
@RestController
@RequestMapping("/api/entity-channel")
@Api(tags = "实体渠道配置")
public class EntityChannelController {

    @Autowired
    private EntityChannelService entityChannelService;

    /**
     * 分页查询实体渠道配置列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询实体渠道配置列表")
    @RequiresPermissions("bi:entity:query")
    public Result<PageResult<EntityChannelVO>> pageEntityChannels(PageQuery pageQuery, EntityChannelQueryDTO queryDTO) {
        PageResult<EntityChannelVO> pageResult = entityChannelService.pageEntityChannels(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询实体渠道配置
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询实体渠道配置")
    @RequiresPermissions("bi:entity:query")
    public Result<EntityChannelVO> getById(@PathVariable String id) {
        EntityChannelVO vo = entityChannelService.getEntityChannelVOById(id);
        if (vo == null) {
            return Result.notFound("实体渠道配置不存在");
        }
        return Result.success(vo);
    }

    /**
     * 新增实体渠道配置
     */
    @PostMapping
    @ApiOperation("新增实体渠道配置")
    @RequiresPermissions("bi:entity:add")
    public Result<?> add(@Valid @RequestBody EntityChannelDTO dto) {
        try {
            boolean success = entityChannelService.addEntityChannel(dto);
            return success ? Result.success("新增成功") : Result.error("新增失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改实体渠道配置（只允许修改渠道属性）
     */
    @PutMapping("/{id}")
    @ApiOperation("修改实体渠道配置")
    @RequiresPermissions("bi:entity:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody EntityChannelDTO dto) {
        dto.setId(id);
        try {
            boolean success = entityChannelService.updateEntityChannel(dto);
            return success ? Result.success("修改成功") : Result.error("修改失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除实体渠道配置（逻辑删除完成）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除实体渠道配置")
    @RequiresPermissions("bi:entity:delete")
    public Result<?> delete(@PathVariable String id) {
        boolean success = entityChannelService.deleteEntityChannel(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
