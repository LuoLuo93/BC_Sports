package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.ErpEmployeeSyncStatus;
import com.bcsport.admin.vo.ErpEmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ErpEmployeeSyncStatusMapper extends BaseMapper<ErpEmployeeSyncStatus> {

    List<String> selectShopDepartmentIds();

    List<ErpEmployeeVO> selectOnboardingPage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    long countOnboarding(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds
    );

    List<ErpEmployeeVO> selectUpdatePage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    long countUpdate(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds
    );

    List<ErpEmployeeVO> selectLeavingPage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    long countLeaving(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds
    );

    void upsertByEmployeeIdAndType(@Param("entity") ErpEmployeeSyncStatus entity);
}
