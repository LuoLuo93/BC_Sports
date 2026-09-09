package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.SysImportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一导入日志（主库）
 */
@Mapper
public interface SysImportLogMapper extends BaseMapper<SysImportLog> {
}
