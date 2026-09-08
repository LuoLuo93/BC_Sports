package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxCustomerlistdetailsFollowInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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
     * 影子表操作：全量同步先写 STG，成功后与主表 deleteAll 同一事务切换，失败保留主表旧数据
     */
    void clearStg();
    void insertBatchStg(@Param("list") List<VxCustomerlistdetailsFollowInfo> list);
    void copyFromStg();

    /**
     * 查询所有数据
     */
    List<VxCustomerlistdetailsFollowInfo> selectAll();

    List<VxCustomerlistdetailsFollowInfo> selectByExternalUserids(@Param("externalUserids") List<String> externalUserids);

    /**
     * 分页查询某成员跟进的客户（关联外部联系人表获取客户名称/类型）
     */
    List<Map<String, Object>> selectCustomerPageByUserid(@Param("userid") String userid,
                                                         @Param("name") String name,
                                                         @Param("offset") int offset,
                                                         @Param("pageSize") int pageSize);

    /**
     * 查询某成员跟进的客户总数
     */
    long selectCustomerCountByUserid(@Param("userid") String userid,
                                     @Param("name") String name);

}
