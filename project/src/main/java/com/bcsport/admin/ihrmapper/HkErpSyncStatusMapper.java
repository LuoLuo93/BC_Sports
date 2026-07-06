package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.HkErpSyncStatus;
import com.bcsport.admin.vo.ErpEmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * HK ERP直写链路同步状态 Mapper（BC_SPORTS_IHR 库）
 * <p>
 * 数据源与伯俊链路一致（employee_additions / employee_modifications / employee_information），
 * 仅同步状态独立记录到 hk_erp_sync_status，sync_type 用 HK_ 前缀。
 */
@Mapper
public interface HkErpSyncStatusMapper extends BaseMapper<HkErpSyncStatus> {

    /** 递归查询终端店铺下的所有部门ID（与伯俊链路共用同一套部门过滤逻辑） */
    List<String> selectShopDepartmentIds();

    // ==================== 待同步查询（syncAll 使用） ====================

    /** 待同步入职：终端店铺部门 + 排除已离职 + 有效状态 IN(0,2,null)，含"数据更新后重置为未同步"判定 */
    List<ErpEmployeeVO> selectPendingHkOnboardings(@Param("shopDeptIds") List<String> shopDeptIds);

    /** 待同步变更：终端店铺部门 + 有效状态 */
    List<ErpEmployeeVO> selectPendingHkUpdates(@Param("shopDeptIds") List<String> shopDeptIds);

    /** 待同步离职：leave_date 非空 + 终端店铺部门 + 有效状态 */
    List<ErpEmployeeVO> selectPendingHkLeavings(@Param("shopDeptIds") List<String> shopDeptIds);

    // ==================== 分页查询（前端 Tab 展示） ====================

    List<ErpEmployeeVO> selectHkOnboardingPage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds,
            @Param("offset") long offset,
            @Param("limit") long limit);

    long countHkOnboarding(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds);

    List<ErpEmployeeVO> selectHkUpdatePage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds,
            @Param("offset") long offset,
            @Param("limit") long limit);

    long countHkUpdate(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds);

    List<ErpEmployeeVO> selectHkLeavingPage(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds,
            @Param("offset") long offset,
            @Param("limit") long limit);

    long countHkLeaving(
            @Param("staffName") String staffName,
            @Param("staffNo") String staffNo,
            @Param("syncStatus") Integer syncStatus,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("excludedStaffNos") List<String> excludedStaffNos,
            @Param("shopDeptIds") List<String> shopDeptIds);

    // ==================== Upsert ====================

    /** MERGE upsert：按 (employee_id, sync_type) 唯一键更新或插入 */
    void upsertByEmployeeIdAndType(@Param("entity") HkErpSyncStatus entity);
}
