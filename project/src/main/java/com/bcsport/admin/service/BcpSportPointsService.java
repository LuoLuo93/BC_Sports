package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bcp.BcpSportPoints;
import com.bcsport.admin.entity.SysImportLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface BcpSportPointsService {

    /**
     * 分页查询运动积分
     */
    PageResult<BcpSportPoints> page(PageQuery pageQuery, String sporter);

    /**
     * Excel 导入运动积分
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页
     */
    PageResult<SysImportLog> logPage(PageQuery pageQuery);
}
