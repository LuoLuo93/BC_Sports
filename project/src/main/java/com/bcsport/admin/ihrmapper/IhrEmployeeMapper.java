package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeMapper extends BaseMapper<IhrEmployee> {
    void insertBatch(@Param("list") List<IhrEmployee> list);
    void deleteAll();
}
