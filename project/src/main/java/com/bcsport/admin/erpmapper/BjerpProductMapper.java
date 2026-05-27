package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BjerpProductMapper {
    List<Map<String, Object>> searchProducts(@Param("materialNumber") String materialNumber, @Param("styleNumber") String styleNumber, @Param("materialName") String materialName, @Param("brandId") String brandId);

    Map<String, Object> getProductByEan13(@Param("ean13") String ean13);

    List<Map<String, Object>> getBrands();
}
