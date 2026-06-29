package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.BasFirstAdd;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户首次添加记录 Mapper
 */
@Mapper
public interface BasFirstAddMapper extends BaseMapper<BasFirstAdd> {

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<BasFirstAdd> list);

    /**
     * MERGE upsert：一条 SQL 同时处理插入和更新（SQL Server 专用）
     */
    void mergeBatch(@Param("list") List<BasFirstAdd> list);
}

