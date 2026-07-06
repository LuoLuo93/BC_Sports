package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.HkEmployeeQueryDTO;
import com.bcsport.admin.vo.ErpEmployeeVO;
import com.bcsport.admin.vo.HkSyncStatsVO;

/**
 * 旧版HK ERP 职员资料同步服务
 * <p>
 * 数据源：BC_SPORTS_IHR 库（employee_additions / employee_modifications / employee_information）
 * 部门过滤：BC_SPORTS_IHR.department 的"终端店铺"递归
 * 写入目标：HKERP 库 Bas_Personnel（直写）
 * 同步状态：BC_SPORTS_IHR 库 hk_erp_sync_status（sync_type 用 HK_ 前缀，与伯俊链路隔离）
 */
public interface HkPersonnelSyncService {

    /**
     * 新员工入职同步
     */
    HkSyncStatsVO syncNewPersonnel();

    /**
     * 员工变更 + 离职同步
     */
    HkSyncStatsVO syncPersonnelUpdate();

    /**
     * 单条同步（供前端单条触发）
     *
     * @param syncType   HK_ONBOARDING / HK_UPDATE / HK_LEAVING
     * @param employeeId IHR员工ID
     * @return null=成功，非空=错误信息
     */
    String syncSingle(String syncType, String employeeId);

    // ==================== 分页查询（前端 Tab 展示） ====================

    PageResult<ErpEmployeeVO> pageHkOnboardings(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO);

    PageResult<ErpEmployeeVO> pageHkUpdates(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO);

    PageResult<ErpEmployeeVO> pageHkLeavings(PageQuery pageQuery, HkEmployeeQueryDTO queryDTO);
}
