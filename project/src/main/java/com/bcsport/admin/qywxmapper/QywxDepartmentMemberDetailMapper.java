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
     * 影子表操作：全量同步先写 STG，成功后与主表 deleteAll 同一事务切换，失败保留主表旧数据
     */
    void clearStg();
    void insertBatchStg(@Param("list") List<QywxDepartmentMemberDetail> list);
    void copyFromStg();

    /**
     * 根据用户ID查找姓名
     */
    String selectNameByUserid(@Param("userid") String userid);

    /**
     * 根据用户ID批量查找姓名
     */
    List<QywxDepartmentMemberDetail> selectNameByUserids(@Param("userids") List<String> userids);
}
