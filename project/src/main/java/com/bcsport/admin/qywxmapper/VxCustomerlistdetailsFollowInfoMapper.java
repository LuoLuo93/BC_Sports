package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsFollowInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信外部联系人跟进信息Mapper
 */
@Mapper
public interface VxCustomerlistdetailsFollowInfoMapper {

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<VxCustomerlistdetailsFollowInfo> list);

    /**
     * 查询所有数据
     */
    List<VxCustomerlistdetailsFollowInfo> selectAll();

    List<VxCustomerlistdetailsFollowInfo> selectByExternalUserids(@Param("externalUserids") List<String> externalUserids);

}
