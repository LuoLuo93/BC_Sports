package com.bcsport.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.RegionDTO;
import com.bcsport.admin.dto.RegionQueryDTO;
import com.bcsport.admin.service.RegionService;
import com.bcsport.admin.vo.RegionVO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/region")
public class RegionController {

    @Autowired
    private RegionService regionService;

    @GetMapping("/tree")
    @RequiresPermissions("bi:region:query")
    public Result<List<RegionVO>> listTree(RegionQueryDTO query) {
        List<RegionVO> tree = regionService.listByTree(query);
        return Result.success(tree);
    }

    @GetMapping("/page")
    @RequiresPermissions("bi:region:query")
    public Result<IPage<RegionVO>> listPage(RegionQueryDTO query) {
        IPage<RegionVO> page = regionService.listByPage(query);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("bi:region:query")
    public Result<RegionVO> getById(@PathVariable String id) {
        RegionVO region = regionService.getRegionVOById(id);
        if (region == null) {
            return Result.notFound("地区不存在");
        }
        return Result.success(region);
    }

    @PostMapping
    @RequiresPermissions("bi:region:add")
    public Result<String> add(@Valid @RequestBody RegionDTO regionDTO) {
        regionService.addRegion(regionDTO);
        return Result.success("地区添加成功");
    }

    @PutMapping("/{id}")
    @RequiresPermissions("bi:region:edit")
    public Result<String> update(@PathVariable String id, @Valid @RequestBody RegionDTO regionDTO) {
        regionDTO.setId(id);
        regionService.updateRegion(regionDTO);
        return Result.success("地区更新成功");
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("bi:region:delete")
    public Result<String> delete(@PathVariable String id) {
        regionService.deleteRegion(id);
        return Result.success("地区已成功删除");
    }
}
