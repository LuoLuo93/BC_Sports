package com.bcsport.admin.bidwmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bi.SalesBudgetFillDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺日预算 Mapper（走 bidw 数据源，BI_DW schema）
 * 放在 bidwmapper 包下，由 BidwDataSourceConfig 自动绑定 bidw 数据源
 */
@Mapper
public interface SalesBudgetFillDailyMapper extends BaseMapper<SalesBudgetFillDaily> {

    /**
     * 批量 MERGE upsert（店铺+品牌+预算日期 去重，存在更新金额与维度，不存在插入）
     */
    void mergeBatch(@Param("list") List<SalesBudgetFillDaily> list);
}
