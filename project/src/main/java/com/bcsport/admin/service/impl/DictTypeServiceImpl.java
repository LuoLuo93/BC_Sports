package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.DictTypeQueryDTO;
import com.bcsport.admin.entity.DictType;
import com.bcsport.admin.mapper.DictTypeMapper;
import com.bcsport.admin.service.DictTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DictTypeServiceImpl extends ServiceImpl<DictTypeMapper, DictType> implements DictTypeService {

    @Override
    public List<DictType> listAll() {
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictType::getStatus, 1)
               .orderByAsc(DictType::getDictName);
        return list(wrapper);
    }

    @Override
    public IPage<DictType> pageDictType(DictTypeQueryDTO query, int pageNum, int pageSize) {
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictType::getStatus, 1)
               .like(query != null && query.getDictName() != null && !query.getDictName().isBlank(),
                       DictType::getDictName, query == null ? null : query.getDictName())
               .like(query != null && query.getDictType() != null && !query.getDictType().isBlank(),
                       DictType::getDictType, query == null ? null : query.getDictType())
               .orderByAsc(DictType::getDictName);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDictType(DictType dictType) {
        dictType.setStatus(1);
        return save(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictType(DictType dictType) {
        return updateById(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictType(String id) {
        return removeById(id);
    }
}
