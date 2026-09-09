package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.mapper.SysImportLogMapper;
import com.bcsport.admin.service.ImportLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImportLogServiceImpl implements ImportLogService {

    @Autowired
    private SysImportLogMapper sysImportLogMapper;

    @Override
    public PageResult<SysImportLog> page(ImportType type, PageQuery pageQuery) {
        Page<SysImportLog> page = sysImportLogMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<SysImportLog>()
                        .eq(SysImportLog::getImportType, type.getCode())
                        .orderByDesc(SysImportLog::getCreateTime)
                        .orderByDesc(SysImportLog::getId));
        return PageResult.of(page);
    }
}
