package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.BrandDTO;
import com.bcsport.admin.dto.BrandQueryDTO;
import com.bcsport.admin.entity.Brand;
import com.bcsport.admin.service.BrandService;
import com.bcsport.admin.vo.BrandVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import javax.validation.Valid;

/**
 * 品牌管理控制器 */
@RestController
@RequestMapping("/api/brand")
@Api(tags = "品牌管理")
public class BrandController {

    @Autowired
    private BrandService brandService;
    
    @Autowired
    private com.bcsport.admin.mapper.BrandMapper brandMapper;

    /**
     * 分页查询品牌列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询品牌列表")
    @RequiresPermissions("bi:brand:query")
    public Result<PageResult<BrandVO>> pageBrands(PageQuery pageQuery, BrandQueryDTO brandQueryDTO) {
        PageResult<BrandVO> pageResult = brandService.pageBrands(pageQuery, brandQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询所有启用的品牌列表（下拉框使用）     */
    @GetMapping("/list")
    @ApiOperation("查询品牌列表（下拉框使用）")
    @RequiresPermissions("bi:brand:query")
    public Result<List<com.bcsport.admin.vo.BrandVO>> listBrands() {
        return Result.success(brandService.listEnabledBrands());
    }

    /**
     * 根据ID查询品牌
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询品牌")
    @RequiresPermissions("bi:brand:query")
    public Result<BrandVO> getById(@PathVariable String id) {
        BrandVO brandVO = brandService.getBrandVOById(id);
        if (brandVO == null) {
            return Result.notFound("品牌不存在");
        }
        return Result.success(brandVO);
    }

    /**
     * 新增品牌
     */
    @PostMapping
    @ApiOperation("新增品牌")
    @RequiresPermissions("bi:brand:add")
    public Result<?> add(@Valid @RequestBody BrandDTO brandDTO) {
        // 使用数据库序列生成递增 ID
        if (brandDTO.getId() == null || brandDTO.getId().isEmpty()) {
            brandDTO.setId(String.valueOf(brandMapper.selectNextId()));
        }
        boolean success = brandService.addBrand(brandDTO);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 修改品牌
     */
    @PutMapping("/{id}")
    @ApiOperation("修改品牌")
    @RequiresPermissions("bi:brand:edit")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody BrandDTO brandDTO) {
        brandDTO.setId(id);
        boolean success = brandService.updateBrand(brandDTO);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 删除品牌
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除品牌")
    @RequiresPermissions("bi:brand:delete")
    public Result<?> delete(@PathVariable String id) {
        // 使用 MyBatis-Plus 的逻辑删除
        boolean success = brandService.removeById(id);
        return success ? Result.success("品牌已被成功移除") : Result.error("未找到对应品牌，移除失败");
    }
}
