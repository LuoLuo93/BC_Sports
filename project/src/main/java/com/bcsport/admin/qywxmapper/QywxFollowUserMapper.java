package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxFollowUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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

    /**
     * 分页查询客户联系成员（关联成员详情表获取姓名/职位/手机）
     */
    List<Map<String, Object>> selectPageWithDetail(@Param("name") String name,
                                                    @Param("mobile") String mobile,
                                                    @Param("mainDepartment") String mainDepartment,
                                                    @Param("offset") int offset,
                                                    @Param("pageSize") int pageSize);

    /**
     * 查询客户联系成员总数
     */
    long selectCountWithDetail(@Param("name") String name,
                               @Param("mobile") String mobile,
                               @Param("mainDepartment") String mainDepartment);
}
