package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门管理Mapper接口
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
}
