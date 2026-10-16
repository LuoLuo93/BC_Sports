package com.bcsport.admin.erpmapper;

import com.bcsport.admin.vo.ErpEmployeeCurrent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 伯俊ERP员工只读查询（HR_EMPLOYEE，走 bjerp 数据源）
 * REST v3 协议下 ObjectModify 需以记录数字 ID 定位（ak 已废弃），
 * 工号→ID 解析与现值比对均在此查询，只读不写。
 */
@Mapper
public interface BjerpEmployeeMapper {

    /**
     * 按工号查员工现值（含店仓名），工号两侧 TRIM 兼容历史数据带空格。
     *
     * @return null=ERP无此工号
     */
    ErpEmployeeCurrent selectByStaffNo(@Param("staffNo") String staffNo);

    /**
     * 店仓名精确匹配计数（ISACTIVE=Y），调店目标校验用：
     * 0=该店名在ERP不存在（IHR部门名与ERP店名漂移，需人工核正）
     */
    int countActiveStoreByName(@Param("storeName") String storeName);
}
