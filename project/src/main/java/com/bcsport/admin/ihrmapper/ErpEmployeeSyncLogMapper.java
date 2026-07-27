package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.ErpEmployeeSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP人员同步日志 Mapper
 */
@Mapper
public interface ErpEmployeeSyncLogMapper extends BaseMapper<ErpEmployeeSyncLog> {

    /**
     * 分页查询同步日志（按同步时间倒序）
     */
    List<ErpEmployeeSyncLog> selectLogPage(
            @Param("syncType") String syncType,
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    /**
     * 查询日志总数
     */
    long countLog(
            @Param("syncType") String syncType,
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus
    );

    /**
     * 按 id 查询完整记录（含请求/响应体大字段，用于详情弹窗）
     */
    ErpEmployeeSyncLog selectDetailById(@Param("id") Long id);

    /**
     * 插入一条同步日志
     */
    void insertLog(@Param("entity") ErpEmployeeSyncLog entity);
}
