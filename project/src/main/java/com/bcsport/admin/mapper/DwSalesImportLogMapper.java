package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bi.DwSalesImportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数仓销售导入日志 Mapper（走主库 dataSource，BC_SPORTS schema）
 */
@Mapper
public interface DwSalesImportLogMapper extends BaseMapper<DwSalesImportLog> {
}
