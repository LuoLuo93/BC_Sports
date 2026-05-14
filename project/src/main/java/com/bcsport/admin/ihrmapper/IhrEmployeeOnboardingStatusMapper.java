package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeOnboardingStatus;
import com.bcsport.admin.vo.IhrEmployeeOnboardingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeOnboardingStatusMapper extends BaseMapper<IhrEmployeeOnboardingStatus> {

    List<IhrEmployeeOnboardingVO> selectOnboardingPage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    long countOnboarding(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos
    );

    void upsertByEmployeesId(@Param("entity") IhrEmployeeOnboardingStatus entity);
}
