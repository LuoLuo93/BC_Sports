package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bi.BudgetImportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 店铺日预算导入日志 Mapper（走主库 dataSource，BC_SPORTS schema）
 */
@Mapper
public interface BudgetImportLogMapper extends BaseMapper<BudgetImportLog> {
}
