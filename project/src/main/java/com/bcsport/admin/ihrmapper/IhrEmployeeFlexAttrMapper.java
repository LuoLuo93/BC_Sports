package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrEmployeeFlexAttr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrEmployeeFlexAttrMapper extends BaseMapper<IhrEmployeeFlexAttr> {
    void insertBatch(@Param("list") List<IhrEmployeeFlexAttr> list);
    void deleteAll();

    /**
     * 影子表操作：全量同步先写 STG，成功后与主表 deleteAll 同一事务切换，失败保留主表旧数据
     */
    void clearStg();
    void insertBatchStg(@Param("list") List<IhrEmployeeFlexAttr> list);
    void copyFromStg();
}
