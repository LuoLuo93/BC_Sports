package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.StoreWhitelist;

public interface StoreWhitelistService {

    /**
     * 分页查询店铺白名单（店仓编码/店仓名称模糊过滤）
     */
    PageResult<StoreWhitelist> page(PageQuery pageQuery, String storeCode, String storeName);

    /**
     * 新增一条白名单店铺（店仓编码不能与其他记录重复）
     */
    void add(String storeCode, String storeName);

    /**
     * 编辑一条白名单店铺（店仓编码不能与其他记录重复，换名直接改本行）
     */
    void update(Long id, String storeCode, String storeName);

    /**
     * 删除一条白名单店铺（逻辑删除）
     */
    void delete(Long id);
}
