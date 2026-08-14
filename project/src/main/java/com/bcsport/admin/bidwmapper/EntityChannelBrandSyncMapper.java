package com.bcsport.admin.bidwmapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 实体渠道-店铺品牌同步 Mapper（走 bidw 数据源，BI_DW schema）
 * 放在 bidwmapper 包下，由 BidwDataSourceConfig 自动绑定 bidw 数据源。
 * 从数仓销售(ODS_SALES_MAIN) + 库存(DWD_STOCK_DAILY) 提取 distinct 的 店铺+品牌 组合。
 */
@Mapper
public interface EntityChannelBrandSyncMapper {

    /**
     * 查询数仓中所有出现过的 店铺+品牌 组合（销售 UNION 库存）
     * 关联：
     *   销售 ODS_SALES_MAIN.PRODUCT_CODE = ODS_M_PRODUCT.NAME
     *   库存 DWD_STOCK_DAILY.C_STORE_ID  = ODS_C_STORE.ID (取 CODE)
     *   库存 DWD_STOCK_DAILY.M_PRODUCT_ID = ODS_M_PRODUCT.ID
     *
     * @return 每行 Map：storeCode / storeName / brandName
     */
    List<Map<String, Object>> listDistinctStoreBrand();
}
