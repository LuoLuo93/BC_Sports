package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bcsport.admin.entity.DictData;
import com.bcsport.admin.entity.DictType;
import com.bcsport.admin.service.DictDataService;
import com.bcsport.admin.service.DictTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dict")
@Api(tags = "数据字典")
public class DictController {

    @Autowired
    private DictTypeService dictTypeService;

    @Autowired
    private DictDataService dictDataService;

    // ==================== 字典类型 ====================

    @GetMapping("/type/list")
    @ApiOperation("查询所有字典类型")
    @RequiresPermissions("system:dict:query")
    public Result<List<DictType>> listTypes() {
        return Result.success(dictTypeService.listAll());
    }

    @GetMapping("/type/{id}")
    @ApiOperation("查询字典类型详情")
    @RequiresPermissions("system:dict:query")
    public Result<DictType> getType(@PathVariable String id) {
        return Result.success(dictTypeService.getById(id));
    }

    @PostMapping("/type")
    @ApiOperation("新增字典类型")
    @RequiresPermissions("system:dict:add")
    public Result<Void> addType(@Valid @RequestBody DictType dictType) {
        dictTypeService.addDictType(dictType);
        return Result.success(null);
    }

    @PutMapping("/type")
    @ApiOperation("修改字典类型")
    @RequiresPermissions("system:dict:edit")
    public Result<Void> updateType(@Valid @RequestBody DictType dictType) {
        dictTypeService.updateDictType(dictType);
        return Result.success(null);
    }

    @DeleteMapping("/type/{id}")
    @ApiOperation("删除字典类型")
    @RequiresPermissions("system:dict:delete")
    public Result<Void> deleteType(@PathVariable String id) {
        dictTypeService.deleteDictType(id);
        return Result.success(null);
    }

    // ==================== 字典数据 ====================

    @GetMapping("/data/list")
    @ApiOperation("按字典类型查询字典数据")
    @RequiresPermissions("system:dict:query")
    public Result<List<DictData>> listData(@RequestParam String dictType) {
        return Result.success(dictDataService.listByDictType(dictType));
    }

    @GetMapping("/data/page")
    @ApiOperation("分页查询字典数据")
    @RequiresPermissions("system:dict:query")
    public Result<PageResult<DictData>> pageData(
            @RequestParam String dictType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<DictData> page = dictDataService.pageByDictType(dictType, pageNum, pageSize);
        return Result.success(new PageResult<>(page));
    }

    @GetMapping("/data/{id}")
    @ApiOperation("查询字典数据详情")
    @RequiresPermissions("system:dict:query")
    public Result<DictData> getData(@PathVariable String id) {
        return Result.success(dictDataService.getById(id));
    }

    @PostMapping("/data")
    @ApiOperation("新增字典数据")
    @RequiresPermissions("system:dict:add")
    public Result<Void> addData(@Valid @RequestBody DictData dictData) {
        dictDataService.addDictData(dictData);
        return Result.success(null);
    }

    @PutMapping("/data")
    @ApiOperation("修改字典数据")
    @RequiresPermissions("system:dict:edit")
    public Result<Void> updateData(@Valid @RequestBody DictData dictData) {
        dictDataService.updateDictData(dictData);
        return Result.success(null);
    }

    @DeleteMapping("/data/{id}")
    @ApiOperation("删除字典数据")
    @RequiresPermissions("system:dict:delete")
    public Result<Void> deleteData(@PathVariable String id) {
        dictDataService.deleteDictData(id);
        return Result.success(null);
    }
}
