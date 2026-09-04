package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.bi.OdsSalesMain;

/**
 * 数仓销售查看(ODS_SALES_MAIN) Service
 */
public interface OdsSalesMainService {

    /**
     * 分页查询销售主明细（条件均可选，默认不限制）
     */
    PageResult<OdsSalesMain> page(PageQuery pageQuery, OdsSalesMainQueryDTO queryDTO);

    /**
     * 按单据号+明细ID更新归属维度字段
     * @return false=行不存在(可能已被ETL重灌)
     */
    boolean update(OdsSalesMainUpdateDTO dto);
}
