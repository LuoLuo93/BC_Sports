package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeesAuxiliary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeesAuxiliaryMapper extends BaseMapper<IhrEmployeesAuxiliary> {
    void insertBatch(@Param("list") List<IhrEmployeesAuxiliary> list);
    void deleteAll();
}
