package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeModification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface IhrEmployeeModificationMapper extends BaseMapper<IhrEmployeeModification> {
    void insertBatch(@Param("list") List<IhrEmployeeModification> list);
    void deleteBySyncDate(@Param("syncDate") Date syncDate);
}
