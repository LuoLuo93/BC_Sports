package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpShopDTO;
import com.bcsport.admin.dto.ErpShopQueryDTO;
import com.bcsport.admin.entity.ErpShop;
import com.bcsport.admin.mapper.ErpShopMapper;
import com.bcsport.admin.service.ErpShopService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ErpShopVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 店铺服务实现类
 */
@Service
public class ErpShopServiceImpl extends ServiceImpl<ErpShopMapper, ErpShop> implements ErpShopService {

    @Override
    public PageResult<ErpShopVO> pageErpShops(PageQuery pageQuery, ErpShopQueryDTO queryDTO) {
        Page<ErpShop> page = pageQuery.toPage();

        LambdaQueryWrapper<ErpShop> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getShopCode())) {
                queryWrapper.like(ErpShop::getShopCode, queryDTO.getShopCode());
            }
            if (StringUtils.hasText(queryDTO.getShopName())) {
                queryWrapper.like(ErpShop::getShopName, queryDTO.getShopName());
            }
            if (StringUtils.hasText(queryDTO.getShopType())) {
                queryWrapper.eq(ErpShop::getShopType, queryDTO.getShopType());
            }
            if (StringUtils.hasText(queryDTO.getProvince())) {
                queryWrapper.like(ErpShop::getProvince, queryDTO.getProvince());
            }
            if (StringUtils.hasText(queryDTO.getCity())) {
                queryWrapper.like(ErpShop::getCity, queryDTO.getCity());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(ErpShop::getStatus, queryDTO.getStatus());
            }
        }
        queryWrapper.eq(ErpShop::getDeleted, 0);

        // 默认排序
        if (page.orders().isEmpty()) {
            queryWrapper.orderByAsc(ErpShop::getSort).orderByDesc(ErpShop::getCreateTime);
        }

        Page<ErpShop> shopPage = baseMapper.selectPage(page, queryWrapper);
        List<ErpShopVO> voList = shopPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        PageResult<ErpShopVO> result = new PageResult<>();
        result.setPageNum(shopPage.getCurrent());
        result.setPageSize(shopPage.getSize());
        result.setTotal(shopPage.getTotal());
        result.setPages(shopPage.getPages());
        result.setRecords(voList);
        result.setHasPrevious(shopPage.getCurrent() > 1);
        result.setHasNext(shopPage.getCurrent() < shopPage.getPages());
        return result;
    }

    @Override
    public ErpShopVO getErpShopVOById(String id) {
        ErpShop shop = getById(id);
        return convertToVO(shop);
    }

    @Override
    public boolean addErpShop(ErpShopDTO dto) {
        ErpShop shop = BeanCopyUtils.copy(dto, ErpShop.class);
        return save(shop);
    }

    @Override
    public boolean updateErpShop(ErpShopDTO dto) {
        ErpShop shop = BeanCopyUtils.copy(dto, ErpShop.class);
        return updateById(shop);
    }

    @Override
    public List<ErpShopVO> listEnabledErpShops() {
        LambdaQueryWrapper<ErpShop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErpShop::getStatus, 1);
        queryWrapper.eq(ErpShop::getDeleted, 0);
        queryWrapper.orderByAsc(ErpShop::getSort);
        List<ErpShop> list = list(queryWrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO 并设置显示名称
     */
    private ErpShopVO convertToVO(ErpShop shop) {
        if (shop == null) {
            return null;
        }
        ErpShopVO vo = BeanCopyUtils.copy(shop, ErpShopVO.class);
        // 设置店铺类型名称
        if ("online".equals(shop.getShopType())) {
            vo.setShopTypeName("线上店铺");
        } else if ("offline".equals(shop.getShopType())) {
            vo.setShopTypeName("线下店铺");
        } else {
            vo.setShopTypeName("未知");
        }
        // 设置状态名称
        vo.setStatusName(shop.getStatus() != null && shop.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }
}
