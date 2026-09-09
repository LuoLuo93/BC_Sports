package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.OdsSalesMainMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.service.ImportLogService;
import com.bcsport.admin.service.OdsSalesMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数仓销售查看实现（查询/编辑走 bidw 数据源）
 * Excel 期初导入功能已按用户决定删除（2026-09-09，期初数据已灌入，后续走 ETL）；
 * 导入日志保留历史查询，读统一日志表 BC_SPORTS_SYS_IMPORT_LOG（type=DW_SALES）。
 */
@Service
public class OdsSalesMainServiceImpl implements OdsSalesMainService {

    @Autowired
    private OdsSalesMainMapper odsSalesMainMapper;

    @Autowired
    private ImportLogService importLogService;

    @Override
    public PageResult<OdsSalesMain> page(PageQuery pageQuery, OdsSalesMainQueryDTO queryDTO) {
        Page<OdsSalesMain> result = odsSalesMainMapper.selectPage(pageQuery.toPage(), queryDTO);
        return PageResult.of(result);
    }

    @Override
    public boolean update(OdsSalesMainUpdateDTO dto) {
        return odsSalesMainMapper.updateRow(dto) > 0;
    }

    @Override
    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.DW_SALES, pageQuery);
    }
}
