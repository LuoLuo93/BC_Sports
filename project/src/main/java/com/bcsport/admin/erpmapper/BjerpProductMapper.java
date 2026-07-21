package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BjerpProductMapper {
    long countProducts(@Param("materialNumber") String materialNumber, @Param("styleNumber") String styleNumber, @Param("materialName") String materialName, @Param("brandId") String brandId);

    List<Map<String, Object>> searchProducts(@Param("materialNumber") String materialNumber, @Param("styleNumber") String styleNumber, @Param("materialName") String materialName, @Param("brandId") String brandId, @Param("offset") long offset, @Param("pageSize") long pageSize);

    Map<String, Object> getProductByEan13(@Param("ean13") String ean13);

    List<Map<String, Object>> getBrands();

    List<Map<String, Object>> getKinds();

    /** 按 productId 查询该商品每个尺码对应的条码(M_PRODUCT_ALIAS) */
    List<Map<String, Object>> getProductSizes(@Param("productId") String productId);

    /**
     * 按货号(name)更新 M_PRODUCT 的 4 个材质字段。
     * 用于「贴纸资料维护」详情页保存，字段在 ERP 当前为 NULL，首次写入即初始化。
     * @return 受影响行数（0=货号不存在）
     */
    int updateMaterialFields(@Param("materialNumber") String materialNumber,
                             @Param("fabCode") String fabCode,
                             @Param("fabElement") String fabElement,
                             @Param("acCode") String acCode,
                             @Param("accElement") String accElement);
}
