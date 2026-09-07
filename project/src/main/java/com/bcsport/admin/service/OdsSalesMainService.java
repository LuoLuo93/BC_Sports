package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.bi.DwSalesImportLog;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    /**
     * Excel 期初数据导入：SAX 流式解析 + 分批纯INSERT(不防重)
     * BILL_ID/ITEM_ID 空缺时分块取号自动生成(BILL_ID按单据号分组共号)
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页
     */
    PageResult<DwSalesImportLog> logPage(PageQuery pageQuery);
}
