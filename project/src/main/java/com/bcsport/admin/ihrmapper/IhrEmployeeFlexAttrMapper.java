package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeFlexAttr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeFlexAttrMapper extends BaseMapper<IhrEmployeeFlexAttr> {
    void insertBatch(@Param("list") List<IhrEmployeeFlexAttr> list);
    void deleteAll();
}
