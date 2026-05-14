package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.VxCustomerTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VxCustomerTagMapper extends BaseMapper<VxCustomerTag> {

    void insertBatch(@Param("list") List<VxCustomerTag> list);

    List<VxCustomerTag> selectByExternalUserid(@Param("externalUserid") String externalUserid);

    List<String> selectExistingTags(@Param("externalUserid") String externalUserid, @Param("userid") String userid);
}
