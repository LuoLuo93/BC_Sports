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
}
