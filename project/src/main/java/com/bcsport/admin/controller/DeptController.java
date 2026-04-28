package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.DeptDTO;
import com.bcsport.admin.dto.DeptQueryDTO;
import com.bcsport.admin.service.DeptService;
import com.bcsport.admin.vo.DeptVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器 */
@RestController
@RequestMapping("/api/dept")
@Api(tags = "部门管理")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 获取部门列表（树状）
     */
    @GetMapping("/list")
    @ApiOperation("获取部门列表")
    @RequiresPermissions("dept:query")
    public Result<List<DeptVO>> list(DeptQueryDTO deptQueryDTO) {
        List<DeptVO> depts = deptService.selectDeptTree(deptQueryDTO);
        return Result.success(depts);
    }

    /**
     * 根据ID查询部门
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询部门")
    @RequiresPermissions("dept:query")
    public Result<DeptVO> getById(@PathVariable String id) {
        DeptVO deptVO = deptService.getDeptVOById(id);
        if (deptVO == null) {
            return Result.notFound("部门不存在");
        }
        return Result.success(deptVO);
    }

    /**
     * 新增部门
     */
    @PostMapping
    @ApiOperation("新增部门")
    @RequiresPermissions("dept:add")
    public Result<?> add(@Validated @RequestBody DeptDTO deptDTO) {
        if (deptService.addDept(deptDTO)) {
            return Result.success("新增部门成功", null);
        }
        return Result.error("新增部门失败");
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @ApiOperation("更新部门")
    @RequiresPermissions("dept:edit")
    public Result<?> update(@PathVariable String id, @Validated @RequestBody DeptDTO deptDTO) {
        deptDTO.setId(id);
        if (deptService.updateDept(deptDTO)) {
            return Result.success("更新部门成功", null);
        }
        return Result.error("更新部门失败");
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除部门")
    @RequiresPermissions("dept:delete")
    public Result<?> delete(@PathVariable String id) {
        // 检查是否有子部门
        if (deptService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bcsport.admin.entity.Dept>()
                .eq(com.bcsport.admin.entity.Dept::getParentId, id)) > 0) {
            return Result.error("存在下级部门，不允许删除");
        }
        
        if (deptService.removeById(id)) {
            return Result.success("删除部门成功", null);
        }
        return Result.error("删除部门失败");
    }
}
