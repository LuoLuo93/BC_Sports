package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxDepartmentMemberDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信部门成员详情Mapper
 */
@Mapper
public interface QywxDepartmentMemberDetailMapper {

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<QywxDepartmentMemberDetail> list);

    /**
     * 根据用户ID查找姓名
     */
    String selectNameByUserid(@Param("userid") String userid);

    /**
     * 根据用户ID批量查找姓名
     */
    List<QywxDepartmentMemberDetail> selectNameByUserids(@Param("userids") List<String> userids);
}
