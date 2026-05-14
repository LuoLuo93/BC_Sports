package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeAddition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface IhrEmployeeAdditionMapper extends BaseMapper<IhrEmployeeAddition> {
    void insertBatch(@Param("list") List<IhrEmployeeAddition> list);
    void deleteBySyncDate(@Param("syncDate") Date syncDate);
    List<String> selectBySyncDates(@Param("syncDates") List<Date> syncDates);
}
