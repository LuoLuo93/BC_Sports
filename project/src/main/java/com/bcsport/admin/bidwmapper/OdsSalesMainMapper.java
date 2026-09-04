package com.bcsport.admin.bidwmapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数仓销售查看 Mapper（走 bidw 数据源，BI_DW schema）
 * 放在 bidwmapper 包下，由 BidwDataSourceConfig 自动绑定 bidw 数据源
 * ODS_SALES_MAIN 无主键，仅分页查询，不继承 BaseMapper
 */
@Mapper
public interface OdsSalesMainMapper {

    /**
     * 分页查询销售主明细（单据号模糊 + 提交时间范围，条件均可选）
     */
    Page<OdsSalesMain> selectPage(Page<OdsSalesMain> page, @Param("q") OdsSalesMainQueryDTO query);

    /**
     * 按单据号+明细ID更新归属维度字段，返回影响行数(0=行不存在或已被ETL重灌)
     */
    int updateRow(@Param("e") OdsSalesMainUpdateDTO dto);
}
