package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpWarehouseDTO;
import com.bcsport.admin.dto.ErpWarehouseQueryDTO;
import com.bcsport.admin.entity.ErpWarehouse;
import com.bcsport.admin.mapper.ErpWarehouseMapper;
import com.bcsport.admin.service.ErpWarehouseService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ErpWarehouseVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 仓库服务实现类
 */
@Service
public class ErpWarehouseServiceImpl extends ServiceImpl<ErpWarehouseMapper, ErpWarehouse> implements ErpWarehouseService {

    @Override
    public PageResult<ErpWarehouseVO> pageErpWarehouses(PageQuery pageQuery, ErpWarehouseQueryDTO queryDTO) {
        Page<ErpWarehouse> page = pageQuery.toPage();

        LambdaQueryWrapper<ErpWarehouse> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getWarehouseCode())) {
                queryWrapper.like(ErpWarehouse::getWarehouseCode, queryDTO.getWarehouseCode());
            }
            if (StringUtils.hasText(queryDTO.getWarehouseName())) {
                queryWrapper.like(ErpWarehouse::getWarehouseName, queryDTO.getWarehouseName());
            }
            if (StringUtils.hasText(queryDTO.getWarehouseType())) {
                queryWrapper.eq(ErpWarehouse::getWarehouseType, queryDTO.getWarehouseType());
            }
            if (StringUtils.hasText(queryDTO.getProvince())) {
                queryWrapper.like(ErpWarehouse::getProvince, queryDTO.getProvince());
            }
            if (StringUtils.hasText(queryDTO.getCity())) {
                queryWrapper.like(ErpWarehouse::getCity, queryDTO.getCity());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(ErpWarehouse::getStatus, queryDTO.getStatus());
            }
        }
        queryWrapper.eq(ErpWarehouse::getDeleted, 0);

        // 默认排序
        if (page.orders().isEmpty()) {
            queryWrapper.orderByAsc(ErpWarehouse::getSort).orderByDesc(ErpWarehouse::getCreateTime);
        }

        Page<ErpWarehouse> warehousePage = baseMapper.selectPage(page, queryWrapper);
        List<ErpWarehouseVO> voList = warehousePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        PageResult<ErpWarehouseVO> result = new PageResult<>();
        result.setPageNum(warehousePage.getCurrent());
        result.setPageSize(warehousePage.getSize());
        result.setTotal(warehousePage.getTotal());
        result.setPages(warehousePage.getPages());
        result.setRecords(voList);
        result.setHasPrevious(warehousePage.getCurrent() > 1);
        result.setHasNext(warehousePage.getCurrent() < warehousePage.getPages());
        return result;
    }

    @Override
    public ErpWarehouseVO getErpWarehouseVOById(String id) {
        ErpWarehouse warehouse = getById(id);
        return convertToVO(warehouse);
    }

    @Override
    public boolean addErpWarehouse(ErpWarehouseDTO dto) {
        ErpWarehouse warehouse = BeanCopyUtils.copy(dto, ErpWarehouse.class);
        return save(warehouse);
    }

    @Override
    public boolean updateErpWarehouse(ErpWarehouseDTO dto) {
        ErpWarehouse warehouse = BeanCopyUtils.copy(dto, ErpWarehouse.class);
        return updateById(warehouse);
    }

    @Override
    public List<ErpWarehouseVO> listEnabledErpWarehouses() {
        LambdaQueryWrapper<ErpWarehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErpWarehouse::getStatus, 1);
        queryWrapper.eq(ErpWarehouse::getDeleted, 0);
        queryWrapper.orderByAsc(ErpWarehouse::getSort);
        List<ErpWarehouse> list = list(queryWrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO 并设置显示名称
     */
    private ErpWarehouseVO convertToVO(ErpWarehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        ErpWarehouseVO vo = BeanCopyUtils.copy(warehouse, ErpWarehouseVO.class);
        // 设置仓库类型名称
        switch (warehouse.getWarehouseType()) {
            case "normal":
                vo.setWarehouseTypeName("普通仓");
                break;
            case "cold":
                vo.setWarehouseTypeName("冷链仓");
                break;
            case "bonded":
                vo.setWarehouseTypeName("保税仓");
                break;
            default:
                vo.setWarehouseTypeName("未知");
        }
        // 设置状态名称
        vo.setStatusName(warehouse.getStatus() != null && warehouse.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }
}
