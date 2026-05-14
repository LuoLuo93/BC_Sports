package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeUpdateStatus;
import com.bcsport.admin.vo.IhrEmployeeUpdateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeUpdateStatusMapper extends BaseMapper<IhrEmployeeUpdateStatus> {

    List<IhrEmployeeUpdateVO> selectUpdatePage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    long countUpdate(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos
    );

    void upsertByStaffId(@Param("entity") IhrEmployeeUpdateStatus entity);
}
