package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.IhrEmployeeExclusion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeExclusionMapper extends BaseMapper<IhrEmployeeExclusion> {

    /**
     * 检查员工是否在排除列表中（启用状态）
     * @param staffName 员工姓名
     * @param staffNo 工号
     * @param exclusionType 排除类型
     * @return 存在返回 true
     */
    boolean checkExcluded(@Param("staffName") String staffName,
                         @Param("staffNo") String staffNo,
                         @Param("exclusionType") Integer exclusionType);

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<IhrEmployeeExclusion> list);
}
