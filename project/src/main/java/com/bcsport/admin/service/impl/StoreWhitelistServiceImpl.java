package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.entity.bi.StoreWhitelist;
import com.bcsport.admin.erpmapper.BjerpStoreMapper;
import com.bcsport.admin.mapper.StoreWhitelistMapper;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.StoreWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoreWhitelistServiceImpl implements StoreWhitelistService {

    /** 店仓编码长度上限（与表列 VARCHAR2(80 CHAR) 对齐） */
    private static final int STORE_CODE_MAX = 80;

    /** 店仓名称长度上限（与表列 VARCHAR2(255 CHAR) 对齐） */
    private static final int STORE_NAME_MAX = 255;

    /** 自提店铺属性值(C_STORE.C_STOREATTRIB8_ID)的系统配置键与默认值 */
    private static final String ATTRIB8_CONFIG_KEY = "store.whitelist.attrib8Id";
    private static final String ATTRIB8_DEFAULT = "7582";

    @Autowired
    private StoreWhitelistMapper storeWhitelistMapper;

    @Autowired
    private BjerpStoreMapper bjerpStoreMapper;

    @Autowired
    private ConfigService configService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncFromErp(String attribValue) {
        String attrib = StringUtils.hasText(attribValue) ? attribValue.trim()
                : configService.getString(ATTRIB8_CONFIG_KEY, ATTRIB8_DEFAULT);

        // ① 伯俊ERP拉自提店铺(编码+名称)
        List<Map<String, Object>> erpStores = bjerpStoreMapper.listStoresByAttrib8(attrib);

        // ② 现存白名单(逻辑删除自动过滤)按编码索引
        List<StoreWhitelist> current = storeWhitelistMapper.selectList(null);
        Map<String, StoreWhitelist> byCode = current.stream()
                .collect(Collectors.toMap(StoreWhitelist::getStoreCode, Function.identity(), (a, b) -> a));

        // ③ ERP侧: 没有→新增AUTO; MANUAL→跳过(手工行归用户管); AUTO→名称变化才更新
        Set<String> erpCodes = new HashSet<>();
        int inserted = 0, updated = 0, unchanged = 0, manualSkip = 0;
        for (Map<String, Object> s : erpStores) {
            String code = s.get("CODE") == null ? "" : String.valueOf(s.get("CODE")).trim();
            String name = s.get("NAME") == null ? "" : String.valueOf(s.get("NAME")).trim();
            if (code.isEmpty()) {
                continue;
            }
            erpCodes.add(code);
            StoreWhitelist row = byCode.get(code);
            if (row == null) {
                StoreWhitelist entity = new StoreWhitelist();
                entity.setStoreCode(code);
                entity.setStoreName(name);
                entity.setSource(StoreWhitelist.SOURCE_AUTO);
                entity.setCreateBy("system");
                entity.setUpdateBy("system");
                storeWhitelistMapper.insert(entity);
                inserted++;
            } else if (StoreWhitelist.SOURCE_MANUAL.equals(row.getSource())) {
                manualSkip++;
            } else if (!Objects.equals(row.getStoreName(), name)) {
                storeWhitelistMapper.update(null, new LambdaUpdateWrapper<StoreWhitelist>()
                        .eq(StoreWhitelist::getId, row.getId())
                        .set(StoreWhitelist::getStoreName, name)
                        .set(StoreWhitelist::getUpdateBy, "system")
                        .set(StoreWhitelist::getUpdateTime, LocalDateTime.now()));
                updated++;
            } else {
                unchanged++;
            }
        }

        // ④ ERP侧已无自提属性的AUTO行 → 软删移出白名单(MANUAL行不动)
        int removed = 0;
        for (StoreWhitelist row : current) {
            if (StoreWhitelist.SOURCE_AUTO.equals(row.getSource()) && !erpCodes.contains(row.getStoreCode())) {
                storeWhitelistMapper.deleteById(row.getId());
                removed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attrib", attrib);
        result.put("total", erpStores.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("unchanged", unchanged);
        result.put("manualSkip", manualSkip);
        result.put("removed", removed);
        return result;
    }
}
