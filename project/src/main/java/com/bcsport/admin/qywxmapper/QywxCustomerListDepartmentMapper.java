package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxCustomerListDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信客户联系成员部门关系Mapper
 */
@Mapper
public interface QywxCustomerListDepartmentMapper {

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<QywxCustomerListDepartment> list);

    /**
     * 影子表操作：全量同步先写 STG，成功后与主表 deleteAll 同一事务切换，失败保留主表旧数据
     */
    void clearStg();
    void insertBatchStg(@Param("list") List<QywxCustomerListDepartment> list);
    void copyFromStg();
}
