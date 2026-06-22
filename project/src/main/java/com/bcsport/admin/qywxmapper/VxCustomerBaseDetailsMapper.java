package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxCustomerBaseDetails;
import com.bcsport.admin.entity.qywx.VxCustomerBaseDetailsGroupMembers;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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

    /**
     * 分页查询某成员的群聊列表
     */
    List<Map<String, Object>> selectPageByOwner(@Param("owner") String owner,
                                                 @Param("name") String name,
                                                 @Param("offset") int offset,
                                                 @Param("pageSize") int pageSize);

    /**
     * 查询某成员的群聊总数
     */
    long selectCountByOwner(@Param("owner") String owner,
                            @Param("name") String name);

    /**
     * 分页查询所有群聊（关联成员详情表获取群主姓名）
     */
    List<Map<String, Object>> selectPage(@Param("name") String name,
                                          @Param("ownerName") String ownerName,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    /**
     * 查询所有群聊总数
     */
    long selectCount(@Param("name") String name,
                     @Param("ownerName") String ownerName);
}
