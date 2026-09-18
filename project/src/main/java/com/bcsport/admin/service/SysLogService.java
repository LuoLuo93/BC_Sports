package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.SysLogQueryDTO;
import com.bcsport.admin.entity.SysLog;
import com.bcsport.admin.vo.ModuleTreeNode;

import java.util.List;

public interface SysLogService {

    PageResult<SysLog> pageLogs(PageQuery pageQuery, SysLogQueryDTO queryDTO);

    void saveLog(SysLog sysLog);

    int cleanLogs(int days);

    /** 日志中实际出现过的模块(去重)按菜单目录分组后的两级树，供前端筛选下拉动态加载 */
    List<ModuleTreeNode> listModuleTree();
}
