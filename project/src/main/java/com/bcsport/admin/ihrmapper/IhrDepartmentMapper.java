package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrDepartmentMapper extends BaseMapper<IhrDepartment> {
    void insertBatch(@Param("list") List<IhrDepartment> list);
    void deleteAll();
    List<String> selectAncestorNames(@Param("departmentId") Long departmentId);
}
