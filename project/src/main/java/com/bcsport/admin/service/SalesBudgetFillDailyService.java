package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SalesBudgetQueryDTO;
import com.bcsport.admin.entity.bi.BudgetImportLog;
import com.bcsport.admin.entity.bi.SalesBudgetFillDaily;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 店铺日预算 Service
 * 数据查询/导入走 bidw 数据源(BI_DW)，导入日志走主库(BC_SPORTS)
 */
public interface SalesBudgetFillDailyService extends IService<SalesBudgetFillDaily> {

    /**
     * 分页查询店铺日预算
     */
    PageResult<SalesBudgetFillDaily> page(PageQuery pageQuery, SalesBudgetQueryDTO queryDTO);

    /**
     * Excel 批量导入（MERGE 覆盖：店铺+品牌+预算日期 判重）
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页查询
     */
    PageResult<BudgetImportLog> logPage(PageQuery pageQuery);
}
