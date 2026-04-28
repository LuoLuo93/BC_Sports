package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxCustomerBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信群列表 Mapper
 */
@Mapper
public interface VxCustomerBaseMapper {

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<VxCustomerBase> list);

    /**
     * 查询所有群ID
     */
    List<String> selectAllChatIds();
}
