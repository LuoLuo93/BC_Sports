package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeExclusion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeExclusionMapper extends BaseMapper<IhrEmployeeExclusion> {

    boolean checkExcluded(@Param("staffName") String staffName,
                         @Param("staffNo") String staffNo,
                         @Param("exclusionType") Integer exclusionType);

    void insertBatch(@Param("list") List<IhrEmployeeExclusion> list);

    List<String> selectActiveStaffNos(@Param("exclusionType") Integer exclusionType);
}
