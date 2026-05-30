
package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.IhrEmployeeExclusionDTO;
import com.bcsport.admin.dto.IhrEmployeeExclusionQueryDTO;
import com.bcsport.admin.dto.IhrExclusionBatchDTO;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import com.bcsport.admin.vo.IhrEmployeeExclusionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/ihr-exclusion")
@Api(tags = "IHR员工排除管理")
public class IhrEmployeeExclusionController {

    @Autowired
    private IhrEmployeeExclusionService exclusionService;

    @GetMapping("/page")
    @ApiOperation("分页查询排除名单")
    @RequiresPermissions("ihr:exclusion:query")
    public Result<PageResult<IhrEmployeeExclusionVO>> page(PageQuery pageQuery, IhrEmployeeExclusionQueryDTO queryDTO) {
        log.debug("查询IHR排除名单, pageNum={}, pageSize={}", pageQuery.getPageNum(), pageQuery.getPageSize());
        PageResult<IhrEmployeeExclusionVO> pageResult = exclusionService.pageExclusions(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询")
    @RequiresPermissions("ihr:exclusion:query")
    public Result<IhrEmployeeExclusionVO> getById(@PathVariable String id) {
        log.debug("查询IHR排除记录, id={}", id);
        IhrEmployeeExclusionVO vo = exclusionService.getExclusionVOById(id);
        if (vo == null) {
            return Result.notFound("记录不存在");
        }
        return Result.success(vo);
    }

    @PostMapping
    @ApiOperation("新增排除记录")
    @RequiresPermissions("ihr:exclusion:add")
    public Result<?> add(@Valid @RequestBody IhrEmployeeExclusionDTO dto) {
        log.info("新增IHR排除记录, staffName={}, staffNo={}", dto.getStaffName(), dto.getStaffNo());
        boolean success = exclusionService.addExclusion(dto);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    @PutMapping("/{id}")
    @ApiOperation("修改排除记录")
    @RequiresPermissions("ihr:exclusion:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody IhrEmployeeExclusionDTO dto) {
        log.info("更新IHR排除记录, id={}", id);
        dto.setId(id);
        boolean success = exclusionService.updateExclusion(dto);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除排除记录")
    @RequiresPermissions("ihr:exclusion:delete")
    public Result<?> delete(@PathVariable String id) {
        log.info("删除IHR排除记录, id={}", id);
        boolean success = exclusionService.deleteExclusion(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除排除记录")
    @RequiresPermissions("ihr:exclusion:delete")
    public Result<?> batchDelete(@Valid @RequestBody IhrExclusionBatchDTO dto) {
        log.info("批量删除IHR排除记录, count={}", dto.getIds().size());
        boolean success = exclusionService.batchDelete(dto.getIds());
        return success ? Result.success("批量删除成功") : Result.error("批量删除失败");
    }

    @PutMapping("/batch/status")
    @ApiOperation("批量更新状态")
    @RequiresPermissions("ihr:exclusion:edit")
    public Result<?> batchUpdateStatus(@Valid @RequestBody IhrExclusionBatchDTO dto) {
        log.info("批量更新IHR排除记录状态, count={}, targetStatus={}", dto.getIds().size(), dto.getTargetStatus());
        boolean success = exclusionService.batchUpdateStatus(dto.getIds(), dto.getTargetStatus());
        return success ? Result.success("批量更新成功") : Result.error("批量更新失败");
    }
}
