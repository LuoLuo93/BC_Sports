package com.bcsport.admin.controller;

import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.ChannelNatureQueryDTO;
import com.bcsport.admin.dto.ChannelTypeQueryDTO;
import com.bcsport.admin.dto.RegionQueryDTO;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.service.BrandService;
import com.bcsport.admin.service.ChannelNatureService;
import com.bcsport.admin.service.ChannelTypeService;
import com.bcsport.admin.service.DictDataService;
import com.bcsport.admin.service.RegionService;
import com.bcsport.admin.vo.BrandVO;
import com.bcsport.admin.vo.ChannelNatureVO;
import com.bcsport.admin.vo.ChannelTypeVO;
import com.bcsport.admin.vo.RegionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 公共基础数据接口（下拉框/选项列表用）。
 * <p>
 * 这些数据（品牌、类别、字典、渠道类型/性质、地区）是跨模块共享的只读基础数据，
 * 供各业务页面的下拉框使用。统一收口到此 Controller，<b>不加 @RequiresPermissions</b>，
 * 仅经 spaAuth 要求登录即可访问，避免单一模块权限用户在借用下拉的页面收到 403。
 * <p>
 * 数据源与原各业务接口完全一致（品牌/类别均查 ERP M_DIM，其余查各自配置表）。
 */
@RestController
@RequestMapping("/api/common")
public class CommonOptionsController {

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ChannelTypeService channelTypeService;

    @Autowired
    private ChannelNatureService channelNatureService;

    @Autowired
    private RegionService regionService;

    /** ERP 品牌（M_DIM DIM1），与 sticker 各 brands 接口数据源一致 */
    @GetMapping("/brands")
    public Result<List<Map<String, Object>>> brands() {
        return Result.success(bjerpProductMapper.getBrands());
    }

    /** ERP 类别（M_DIM DIM4），与 sticker 各 kinds 接口数据源一致 */
    @GetMapping("/kinds")
    public Result<List<Map<String, Object>>> kinds() {
        return Result.success(bjerpProductMapper.getKinds());
    }

    /** 字典数据（按 dictType 查询启用项），与 /api/dict/data/list 数据源一致 */
    @GetMapping("/dict")
    public Result<?> dict(@RequestParam String dictType) {
        return Result.success(dictDataService.listByDictType(dictType));
    }

    /** 本系统品牌列表（下拉用），与 /api/brand/list 数据源一致 */
    @GetMapping("/brand/list")
    public Result<List<BrandVO>> brandList() {
        return Result.success(brandService.listEnabledBrands());
    }

    /** 渠道类型树（下拉用），与 /api/channel-type/tree 数据源一致 */
    @GetMapping("/channel-type/tree")
    public Result<List<ChannelTypeVO>> channelTypeTree(ChannelTypeQueryDTO query) {
        return Result.success(channelTypeService.listByTree(query));
    }

    /** 渠道性质树（下拉用），与 /api/channel-nature/tree 数据源一致 */
    @GetMapping("/channel-nature/tree")
    public Result<List<ChannelNatureVO>> channelNatureTree(ChannelNatureQueryDTO query) {
        return Result.success(channelNatureService.listByTree(query));
    }

    /** 地区树（下拉用），与 /api/region/tree 数据源一致 */
    @GetMapping("/region/tree")
    public Result<List<RegionVO>> regionTree(RegionQueryDTO query) {
        return Result.success(regionService.listByTree(query));
    }
}
