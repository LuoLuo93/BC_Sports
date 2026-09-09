package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.importer.ImportType;

/**
 * 统一导入日志查询。各模块日志接口保留各自权限注解，只是委托到这里按 type 过滤。
 */
public interface ImportLogService {

    /** 按模块分页，createTime 倒序（历史搬迁行 id 大于新行，不能按 id 排） */
    PageResult<SysImportLog> page(ImportType type, PageQuery pageQuery);
}
