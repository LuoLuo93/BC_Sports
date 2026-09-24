package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.StoreWhitelist;

import java.util.Map;

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

    /**
     * 从伯俊ERP同步自提店铺(C_STOREATTRIB8_ID 自提属性)到白名单。
     * 规则：ERP有→新增(source=AUTO)/改名；ERP摘除属性→软删对应AUTO行；手工行(MANUAL)永不改动。
     *
     * @param attribValue 自提属性值，空则取系统配置 store.whitelist.attrib8Id(默认7582)
     * @return 统计信息 {total, inserted, updated, unchanged, manualSkip, removed, attrib}
     */
    Map<String, Object> syncFromErp(String attribValue);
}
