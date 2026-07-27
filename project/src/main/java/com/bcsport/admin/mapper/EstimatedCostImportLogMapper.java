package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bi.EstimatedCostImportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预估成本导入日志 Mapper（本地主库 Oracle）
 */
@Mapper
public interface EstimatedCostImportLogMapper extends BaseMapper<EstimatedCostImportLog> {
}
