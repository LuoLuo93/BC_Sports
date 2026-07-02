package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bi.GoodsOldNew;
import com.bcsport.admin.entity.bi.GoodsImportLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface GoodsOldNewService {

    /**
     * 分页查询货品资料
     */
    PageResult<GoodsOldNew> page(PageQuery pageQuery, String brand, String articleNo);

    /**
     * Excel 导入货品资料
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页
     */
    PageResult<GoodsImportLog> logPage(PageQuery pageQuery);
}
