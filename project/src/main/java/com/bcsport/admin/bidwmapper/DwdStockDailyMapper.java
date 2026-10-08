package com.bcsport.admin.bidwmapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.dto.DwdStockQueryDTO;
import com.bcsport.admin.entity.bi.DwStoreOption;
import com.bcsport.admin.entity.bi.DwdStockDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数仓库存查询 Mapper（走 bidw 数据源，BI_DW schema）
 * 放在 bidwmapper 包下，由 BidwDataSourceConfig 自动绑定 bidw 数据源
 * 纯查询页面，不继承 BaseMapper
 */
@Mapper
public interface DwdStockDailyMapper {

    /**
     * 分页查询每日库存（billdate 必填；可选店仓/货号/品名；两种粒度：批次明细/商品汇总）
     */
    Page<DwdStockDaily> selectPage(Page<DwdStockDaily> page, @Param("q") DwdStockQueryDTO query);

    /**
     * 店仓下拉选项（主客户 c_customer_id=1 全部店仓）
     */
    List<DwStoreOption> selectStores();
}
