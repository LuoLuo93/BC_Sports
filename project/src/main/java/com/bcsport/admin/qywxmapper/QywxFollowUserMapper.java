package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxFollowUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信配置了客户联系功能的成员Mapper
 */
@Mapper
public interface QywxFollowUserMapper {

    /**
     * 查询所有成员UserID
     */
    List<String> selectAllUserIds();

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<QywxFollowUser> list);
}
