package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.qywx.BasFirstAdd;
import com.bcsport.admin.entity.qywx.BasFirstAddImportLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 客户首次添加记录 Service
 */
public interface BasFirstAddService {

    /**
     * 分页查询
     */
    PageResult<BasFirstAdd> page(PageQuery pageQuery, String customerId, String firstAdder);

    /**
     * 从 Excel 批量导入（按 customer_id 增量去重），并记录导入日志
     *
     * @return { total, success, fail, errors }
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页查询
     */
    PageResult<BasFirstAddImportLog> logPage(PageQuery pageQuery);
}


