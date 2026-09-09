package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.bi.OdsSalesMain;

/**
 * 数仓销售查看(ODS_SALES_MAIN) Service
 * Excel 期初导入功能已删除（2026-09-09，期初数据已灌入，后续走 ETL）；导入日志保留历史查询。
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

    /**
     * 导入日志分页（历史记录，读统一日志表）
     */
    PageResult<SysImportLog> logPage(PageQuery pageQuery);
}
