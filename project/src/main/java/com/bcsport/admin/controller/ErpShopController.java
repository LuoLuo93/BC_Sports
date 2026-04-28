package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ErpShopDTO;
import com.bcsport.admin.dto.ErpShopQueryDTO;
import com.bcsport.admin.entity.ErpShop;
import com.bcsport.admin.mapper.ErpShopMapper;
import com.bcsport.admin.service.ErpShopService;
import com.bcsport.admin.vo.ErpShopVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 店铺管理控制器
 */
@RestController
@RequestMapping("/api/erp-shop")
@Api(tags = "ERP 店铺管理")
public class ErpShopController {

    @Autowired
    private ErpShopService erpShopService;

    @Autowired
    private ErpShopMapper erpShopMapper;

    /**
     * 分页查询店铺列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询店铺列表")
    @RequiresPermissions("bi:erpShop:query")
    public Result<PageResult<ErpShopVO>> pageErpShops(PageQuery pageQuery, ErpShopQueryDTO queryDTO) {
        PageResult<ErpShopVO> pageResult = erpShopService.pageErpShops(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询店铺
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询店铺")
    @RequiresPermissions("bi:erpShop:query")
    public Result<ErpShopVO> getById(@PathVariable String id) {
        ErpShopVO vo = erpShopService.getErpShopVOById(id);
        if (vo == null) {
            return Result.notFound("店铺不存在");
        }
        return Result.success(vo);
    }

    /**
     * 新增店铺
     */
    @PostMapping
    @ApiOperation("新增店铺")
    @RequiresPermissions("bi:erpShop:add")
    public Result<?> add(@Valid @RequestBody ErpShopDTO dto) {
        // 使用数据库序列生成递增 ID
        if (dto.getId() == null || dto.getId().isEmpty()) {
            dto.setId(String.valueOf(erpShopMapper.selectNextId()));
        }
        boolean success = erpShopService.addErpShop(dto);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 修改店铺
     */
    @PutMapping("/{id}")
    @ApiOperation("修改店铺")
    @RequiresPermissions("bi:erpShop:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody ErpShopDTO dto) {
        dto.setId(id);
        boolean success = erpShopService.updateErpShop(dto);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 删除店铺（逻辑删除完成）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除店铺")
    @RequiresPermissions("bi:erpShop:delete")
    public Result<?> delete(@PathVariable String id) {
        // 使用 MyBatis-Plus 的逻辑删除
        boolean success = erpShopService.removeById(id);
        return success ? Result.success("店铺已被成功移除") : Result.error("未找到对应店铺，移除失败");
    }

    /**
     * 查询所有启用状态的店铺
     */
    @GetMapping("/list-enabled")
    @ApiOperation("查询所有启用状态的店铺")
    @RequiresPermissions("bi:erpShop:query")
    public Result<List<ErpShopVO>> listEnabled() {
        List<ErpShopVO> list = erpShopService.listEnabledErpShops();
        return Result.success(list);
    }
}
