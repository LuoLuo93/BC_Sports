package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.dto.WarehouseDTO;
import com.bcsport.admin.entity.Warehouse;
import com.bcsport.admin.mapper.WarehouseMapper;
import com.bcsport.admin.service.WarehouseService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.WarehouseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓库服务实现类
 */
@Slf4j
@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    @Override
    public List<WarehouseVO> listWarehouses() {
        LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Warehouse::getDeleted, 0);
        queryWrapper.orderByAsc(Warehouse::getSort).orderByDesc(Warehouse::getCreateTime);
        List<Warehouse> list = list(queryWrapper);
        return BeanCopyUtils.copyList(list, WarehouseVO.class);
    }

    @Override
    public WarehouseVO getWarehouseVOById(String id) {
        Warehouse warehouse = getById(id);
        if (warehouse == null || warehouse.getDeleted() == 1) {
            return null;
        }
        return BeanCopyUtils.copy(warehouse, WarehouseVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWarehouse(WarehouseDTO warehouseDTO) {
        Warehouse warehouse = BeanCopyUtils.copy(warehouseDTO, Warehouse.class);
        // deleted )MybatisPlusAutoFillHandler 自动填充
        boolean result = save(warehouse);
        if (!result) {
            throw new BusinessException("新增仓库失败，数据库操作异常");
        }
        log.info("新增仓库成功，仓库ID：{}, 仓库名称：{}", warehouse.getId(), warehouse.getWarehouseName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWarehouse(WarehouseDTO warehouseDTO) {
        // 检查是否存)
        if (!lambdaQuery().eq(Warehouse::getId, warehouseDTO.getId()).exists()) {
            throw new BusinessException("更新仓库失败，仓库不存在");
        }
        Warehouse warehouse = BeanCopyUtils.copy(warehouseDTO, Warehouse.class);
        boolean result = updateById(warehouse);
        if (!result) {
            throw new BusinessException("更新仓库失败，数据库操作异常");
        }
        log.info("更新仓库成功，仓库ID：{}", warehouse.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWarehouse(String id) {
        // 检查是否存)
        if (!lambdaQuery().eq(Warehouse::getId, id).exists()) {
            throw new BusinessException("删除仓库失败，仓库不存在");
        }
        // 使用 MyBatis-Plus 的逻辑删除
        boolean result = removeById(id);
        if (!result) {
            throw new BusinessException("删除仓库失败，数据库操作异常");
        }
        log.info("删除仓库成功，仓库ID：{}", id);
        return true;
    }

    @Override
    public List<WarehouseVO> listEnabledWarehouses() {
        LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Warehouse::getStatus, 1);
        queryWrapper.eq(Warehouse::getDeleted, 0);
        queryWrapper.orderByAsc(Warehouse::getSort);
        List<Warehouse> list = list(queryWrapper);
        return BeanCopyUtils.copyList(list, WarehouseVO.class);
    }
}
