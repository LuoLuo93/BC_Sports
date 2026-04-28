package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsExternalContact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信外部联系人基本信息Mapper
 */
@Mapper
public interface VxCustomerlistdetailsExternalContactMapper {

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<VxCustomerlistdetailsExternalContact> list);

}
