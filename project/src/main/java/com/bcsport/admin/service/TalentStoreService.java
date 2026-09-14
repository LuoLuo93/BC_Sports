package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.TalentStore;

public interface TalentStoreService {

    /**
     * 分页查询达人店铺（达人名称/店仓编码/店仓名称模糊过滤）
     */
    PageResult<TalentStore> page(PageQuery pageQuery, String talentName, String storeCode, String storeName);

    /**
     * 新增一条达人店铺绑定（达人+店仓组合不能重复）
     */
    void add(String talentName, String storeCode, String storeName);

    /**
     * 编辑一条达人店铺绑定（改名/换店仓后不能与其他记录组合重复）
     */
    void update(Long id, String talentName, String storeCode, String storeName);

    /**
     * 删除一条达人店铺绑定（逻辑删除）
     */
    void delete(Long id);
}
