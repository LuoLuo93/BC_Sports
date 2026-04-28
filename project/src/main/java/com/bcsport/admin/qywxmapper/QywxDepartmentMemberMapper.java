package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.QywxDepartmentMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QywxDepartmentMemberMapper extends BaseMapper<QywxDepartmentMember> {
    void insertBatch(@Param("list") List<QywxDepartmentMember> list);
    void deleteAll();
    List<String> selectAllUserIds();
}
