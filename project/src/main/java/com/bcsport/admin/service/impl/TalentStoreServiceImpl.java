package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.entity.bi.TalentStore;
import com.bcsport.admin.mapper.TalentStoreMapper;
import com.bcsport.admin.service.TalentStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TalentStoreServiceImpl implements TalentStoreService {

    /** 达人名称长度上限（与表列 VARCHAR2(100 CHAR) 对齐） */
    private static final int TALENT_NAME_MAX = 100;

    @Autowired
    private TalentStoreMapper talentStoreMapper;

    @Override
    public PageResult<TalentStore> page(PageQuery pageQuery, String talentName, String storeCode, String storeName) {
        Page<TalentStore> page = pageQuery.toPage();
        LambdaQueryWrapper<TalentStore> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(talentName)) {
            wrapper.like(TalentStore::getTalentName, talentName.trim());
        }
        if (StringUtils.hasText(storeCode)) {
            wrapper.like(TalentStore::getStoreCode, storeCode.trim());
        }
        if (StringUtils.hasText(storeName)) {
            wrapper.like(TalentStore::getStoreName, storeName.trim());
        }
        wrapper.orderByDesc(TalentStore::getId);
        return PageResult.of(talentStoreMapper.selectPage(page, wrapper));
    }

    @Override
    public void add(String talentName, String storeCode, String storeName) {
        TalentStore entity = buildValidated(talentName, storeCode, storeName);
        checkPairDup(entity.getTalentName(), entity.getStoreCode(), null);
        try {
            talentStoreMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("达人「" + entity.getTalentName() + "」已绑定店仓「" + entity.getStoreName() + "」，不能重复添加");
        }
    }

    @Override
    public void update(Long id, String talentName, String storeCode, String storeName) {
        TalentStore exists = talentStoreMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        TalentStore entity = buildValidated(talentName, storeCode, storeName);
        checkPairDup(entity.getTalentName(), entity.getStoreCode(), id);
        entity.setId(id);
        try {
            talentStoreMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("达人「" + entity.getTalentName() + "」已绑定店仓「" + entity.getStoreName() + "」，不能重复添加");
        }
    }

    @Override
    public void delete(Long id) {
        TalentStore exists = talentStoreMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        talentStoreMapper.deleteById(id);
    }

    /** 三列非空校验 + 去首尾空白，返回待入库实体 */
    private TalentStore buildValidated(String talentName, String storeCode, String storeName) {
        if (!StringUtils.hasText(talentName)) {
            throw new BusinessException("达人名称不能为空");
        }
        if (!StringUtils.hasText(storeCode)) {
            throw new BusinessException("店仓不能为空，请从下拉中选择");
        }
        if (!StringUtils.hasText(storeName)) {
            throw new BusinessException("店仓名称不能为空");
        }
        String name = talentName.trim();
        if (name.length() > TALENT_NAME_MAX) {
            throw new BusinessException("达人名称不能超过 " + TALENT_NAME_MAX + " 个字符");
        }
        TalentStore entity = new TalentStore();
        entity.setTalentName(name);
        entity.setStoreCode(storeCode.trim());
        entity.setStoreName(storeName.trim());
        return entity;
    }

    /** 达人+店仓组合重复预检（excludeId=编辑时排除自身；唯一索引兜底并发窗口） */
    private void checkPairDup(String talentName, String storeCode, Long excludeId) {
        LambdaQueryWrapper<TalentStore> wrapper = new LambdaQueryWrapper<TalentStore>()
                .eq(TalentStore::getTalentName, talentName)
                .eq(TalentStore::getStoreCode, storeCode);
        if (excludeId != null) {
            wrapper.ne(TalentStore::getId, excludeId);
        }
        Long cnt = talentStoreMapper.selectCount(wrapper);
        if (cnt != null && cnt > 0) {
            throw new BusinessException("该达人已绑定此店仓，不能重复添加");
        }
    }
}
