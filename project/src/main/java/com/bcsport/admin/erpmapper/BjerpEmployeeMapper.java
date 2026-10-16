package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 伯俊ERP员工只读查询（HR_EMPLOYEE，走 bjerp 数据源）
 * REST v3 协议下 ObjectModify 需以记录数字 ID 定位（ak 已废弃），
 * 工号→ID 解析在此查询，只读不写。变更报文不做字段级差异比对，
 * 全字段以 IHR 最新数据覆盖，故无需现值快照。
 */
@Mapper
public interface BjerpEmployeeMapper {

    /**
     * 按工号查员工记录ID，工号两侧 TRIM 兼容历史数据带空格。
     *
     * @return null=ERP无此工号（走降级创建）
     */
    Long selectIdByStaffNo(@Param("staffNo") String staffNo);

    /**
     * 店仓名精确匹配计数（ISACTIVE=Y），报文必含店仓字段的前置校验：
     * 0=该店名在ERP不存在（IHR部门名与ERP店名漂移，需人工核正）
     */
    int countActiveStoreByName(@Param("storeName") String storeName);
}
