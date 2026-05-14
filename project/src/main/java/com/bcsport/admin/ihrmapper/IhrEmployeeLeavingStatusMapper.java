package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeLeavingStatus;
import com.bcsport.admin.vo.IhrEmployeeLeavingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeLeavingStatusMapper extends BaseMapper<IhrEmployeeLeavingStatus> {

    List<IhrEmployeeLeavingVO> selectLeavingPage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    long countLeaving(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos
    );

    void upsertByEmployeeId(@Param("entity") IhrEmployeeLeavingStatus entity);
}
