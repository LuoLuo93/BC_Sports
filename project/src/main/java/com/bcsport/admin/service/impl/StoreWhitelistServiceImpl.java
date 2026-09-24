package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.entity.bi.StoreWhitelist;
import com.bcsport.admin.mapper.StoreWhitelistMapper;
import com.bcsport.admin.service.StoreWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StoreWhitelistServiceImpl implements StoreWhitelistService {

    /** 店仓编码长度上限（与表列 VARCHAR2(80 CHAR) 对齐） */
    private static final int STORE_CODE_MAX = 80;

    /** 店仓名称长度上限（与表列 VARCHAR2(255 CHAR) 对齐） */
    private static final int STORE_NAME_MAX = 255;

    @Autowired
    private StoreWhitelistMapper storeWhitelistMapper;

    @Override
    public PageResult<StoreWhitelist> page(PageQuery pageQuery, String storeCode, String storeName) {
        Page<StoreWhitelist> page = pageQuery.toPage();
        LambdaQueryWrapper<StoreWhitelist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(storeCode)) {
            wrapper.like(StoreWhitelist::getStoreCode, storeCode.trim());
        }
        if (StringUtils.hasText(storeName)) {
            wrapper.like(StoreWhitelist::getStoreName, storeName.trim());
        }
        wrapper.orderByDesc(StoreWhitelist::getId);
        return PageResult.of(storeWhitelistMapper.selectPage(page, wrapper));
    }

    @Override
    public void add(String storeCode, String storeName) {
        StoreWhitelist entity = buildValidated(storeCode, storeName);
        checkStoreDup(entity.getStoreCode(), null);
        try {
            storeWhitelistMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("店仓「" + entity.getStoreCode() + "」已在白名单中");
        }
    }

    @Override
    public void update(Long id, String storeCode, String storeName) {
        StoreWhitelist exists = storeWhitelistMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        StoreWhitelist entity = buildValidated(storeCode, storeName);
        checkStoreDup(entity.getStoreCode(), id);
        entity.setId(id);
        try {
            storeWhitelistMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("店仓「" + entity.getStoreCode() + "」已在白名单中");
        }
    }

    @Override
    public void delete(Long id) {
        StoreWhitelist exists = storeWhitelistMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        storeWhitelistMapper.deleteById(id);
    }

    /** 两列非空校验 + 去首尾空白，返回待入库实体 */
    private StoreWhitelist buildValidated(String storeCode, String storeName) {
        if (!StringUtils.hasText(storeCode)) {
            throw new BusinessException("店仓不能为空，请从下拉中选择");
        }
        if (!StringUtils.hasText(storeName)) {
            throw new BusinessException("店仓名称不能为空");
        }
        String code = storeCode.trim();
        String name = storeName.trim();
        if (code.length() > STORE_CODE_MAX) {
            throw new BusinessException("店仓编码不能超过 " + STORE_CODE_MAX + " 个字符");
        }
        if (name.length() > STORE_NAME_MAX) {
            throw new BusinessException("店仓名称不能超过 " + STORE_NAME_MAX + " 个字符");
        }
        StoreWhitelist entity = new StoreWhitelist();
        entity.setStoreCode(code);
        entity.setStoreName(name);
        return entity;
    }

    /** 店仓编码重复预检（excludeId=编辑时排除自身；唯一索引兜底并发窗口） */
    private void checkStoreDup(String storeCode, Long excludeId) {
        LambdaQueryWrapper<StoreWhitelist> wrapper = new LambdaQueryWrapper<StoreWhitelist>()
                .eq(StoreWhitelist::getStoreCode, storeCode);
        if (excludeId != null) {
            wrapper.ne(StoreWhitelist::getId, excludeId);
        }
        Long cnt = storeWhitelistMapper.selectCount(wrapper);
        if (cnt != null && cnt > 0) {
            throw new BusinessException("店仓「" + storeCode + "」已在白名单中");
        }
    }
}
