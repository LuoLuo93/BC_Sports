package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxCustomerBaseDetails;
import com.bcsport.admin.entity.qywx.VxCustomerBaseDetailsGroupMembers;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信群详情 Mapper
 */
@Mapper
public interface VxCustomerBaseDetailsMapper {

    /**
     * 删除群详情数据
     */
    void deleteAllDetails();

    /**
     * 删除群成员数据
     */
    void deleteAllGroupMembers();

    /**
     * 插入群详情
     */
    void insertDetail(VxCustomerBaseDetails detail);

    /**
     * 批量插入群成员
     */
    void insertGroupMembersBatch(@Param("list") List<VxCustomerBaseDetailsGroupMembers> list);
}
