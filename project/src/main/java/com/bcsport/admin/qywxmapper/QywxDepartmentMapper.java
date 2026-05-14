package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.QywxDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QywxDepartmentMapper extends BaseMapper<QywxDepartment> {
    void insertBatch(@Param("list") List<QywxDepartment> list);
    void deleteAll();
    List<QywxDepartment> selectAll();
    List<String> selectDepartIdByName(@Param("name") String name);
    String selectDepartIdByNameAndParent(@Param("name") String name, @Param("parentName") String parentName);
    String selectDepartIdByThreeLevel(@Param("name") String name, @Param("parentName") String parentName, @Param("firstLevelName") String firstLevelName);
}
