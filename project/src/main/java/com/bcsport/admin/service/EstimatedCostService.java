package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.EstimatedCostImportLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface EstimatedCostService {

    /**
     * 分页查询预估成本（伯俊 M_PRODUCT）
     */
    PageResult<Map<String, Object>> page(PageQuery pageQuery, String materialNumber, String styleNumber, String materialName);

    /**
     * 编辑预估成本（UPDATE M_PRODUCT.PRECOST）
     */
    void updatePrecost(String materialNumber, String precost);

    /**
     * Excel 批量导入预估成本（按物料编号更新）
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页
     */
    PageResult<EstimatedCostImportLog> logPage(PageQuery pageQuery);
}
