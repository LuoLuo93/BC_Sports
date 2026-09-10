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

    /**
     * 影子表操作：全量同步先写 STG，成功后与主表 deleteAll 同一事务切换，失败保留主表旧数据
     */
    void clearStg();
    void insertBatchStg(@Param("list") List<VxCustomerlistdetailsExternalContact> list);

    /**
     * 分批回填 STG → 主表（返回本批行数）：externalUserid 拉取时已全局去重可作稳定排序键，
     * 每批一条语句秒级返回，避免整表回填长时间占住 socket 读触发 Read timed out
     */
    int copyFromStgPage(@Param("offset") long offset, @Param("limit") int limit);

}
