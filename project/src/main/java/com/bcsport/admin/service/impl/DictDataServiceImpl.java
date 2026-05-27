package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.entity.DictData;
import com.bcsport.admin.mapper.DictDataMapper;
import com.bcsport.admin.service.DictDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, DictData> implements DictDataService {

    @Override
    public List<DictData> listByDictType(String dictType) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictData::getDictType, dictType)
               .eq(DictData::getStatus, 1)
               .orderByAsc(DictData::getSort);
        return list(wrapper);
    }

    @Override
    public IPage<DictData> pageByDictType(String dictType, int pageNum, int pageSize) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictData::getDictType, dictType)
               .orderByAsc(DictData::getSort);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDictData(DictData dictData) {
        dictData.setStatus(dictData.getStatus() != null ? dictData.getStatus() : 1);
        return save(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictData(DictData dictData) {
        return updateById(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictData(String id) {
        return removeById(id);
    }

    @Override
    public String getLabelByValue(String dictType, String dictValue) {
        return baseMapper.getLabelByValue(dictType, dictValue);
    }
}
