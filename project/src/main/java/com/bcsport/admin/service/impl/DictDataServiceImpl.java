package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.exception.BusinessException;
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
    public IPage<DictData> pageByDictType(String dictType, String dictLabel, String dictValue, int pageNum, int pageSize) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictData::getDictType, dictType)
               .like(dictLabel != null && !dictLabel.isBlank(), DictData::getDictLabel, dictLabel)
               .like(dictValue != null && !dictValue.isBlank(), DictData::getDictValue, dictValue)
               .orderByAsc(DictData::getSort);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDictData(DictData dictData) {
        // 同一字典类型下，字典标签和字典值不可重复
        checkDuplicate(dictData.getDictType(), dictData.getDictLabel(), dictData.getDictValue(), null);
        dictData.setStatus(dictData.getStatus() != null ? dictData.getStatus() : 1);
        return save(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictData(DictData dictData) {
        // 同一字典类型下，字典标签和字典值不可重复（排除自身）
        checkDuplicate(dictData.getDictType(), dictData.getDictLabel(), dictData.getDictValue(), dictData.getId());
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

    /**
     * 校验同一字典类型下字典标签和字典值的唯一性。
     */
    private void checkDuplicate(String dictType, String dictLabel, String dictValue, String excludeId) {
        if (dictType == null) return;
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, dictType);
        if (excludeId != null && !excludeId.isBlank()) {
            wrapper.ne(DictData::getId, excludeId);
        }
        // 检查标签重复
        if (dictLabel != null && !dictLabel.isBlank()) {
            Long labelCount = count(new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictType)
                    .eq(DictData::getDictLabel, dictLabel.trim())
                    .ne(excludeId != null && !excludeId.isBlank(), DictData::getId, excludeId));
            if (labelCount != null && labelCount > 0) {
                throw new BusinessException("字典标签「" + dictLabel + "」在该类型下已存在");
            }
        }
        // 检查值重复
        if (dictValue != null && !dictValue.isBlank()) {
            Long valueCount = count(new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictType)
                    .eq(DictData::getDictValue, dictValue.trim())
                    .ne(excludeId != null && !excludeId.isBlank(), DictData::getId, excludeId));
            if (valueCount != null && valueCount > 0) {
                throw new BusinessException("字典值「" + dictValue + "」在该类型下已存在");
            }
        }
    }
}
