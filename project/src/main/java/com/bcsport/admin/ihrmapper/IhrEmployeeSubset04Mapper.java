package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeSubset04;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeSubset04Mapper extends BaseMapper<IhrEmployeeSubset04> {
    void insertBatch(@Param("list") List<IhrEmployeeSubset04> list);
    void deleteAll();
}
