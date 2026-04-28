package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxAttrsBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业微信成员扩展属性Mapper
 */
@Mapper
public interface QywxAttrsBaseMapper {

    /**
     * 删除所有数据
     */
    void deleteAll();

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<QywxAttrsBase> list);

}
