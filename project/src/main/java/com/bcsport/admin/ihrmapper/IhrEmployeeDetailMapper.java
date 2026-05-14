package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface IhrEmployeeDetailMapper extends BaseMapper<IhrEmployeeDetail> {
    void insertBatch(@Param("list") List<IhrEmployeeDetail> list);
    void deleteAll();
    List<IhrEmployeeDetail> selectRecentLeaved(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    List<IhrEmployeeDetail> selectByStaffIds(@Param("staffIds") List<String> staffIds);
}
