package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 揽众客户押金资料 Mapper（数据源：bjerp LZCUSTOMERINFOR）
 */
@Mapper
public interface LzCustomerMapper {

    long countLzCustomers(@Param("shopCode") String shopCode,
                          @Param("shopName") String shopName,
                          @Param("shopBoss") String shopBoss);

    List<Map<String, Object>> searchLzCustomers(@Param("shopCode") String shopCode,
                                                 @Param("shopName") String shopName,
                                                 @Param("shopBoss") String shopBoss,
                                                 @Param("offset") long offset,
                                                 @Param("pageSize") long pageSize);

    Map<String, Object> getLzCustomerById(@Param("id") Long id);

    int insertLzCustomer(@Param("map") Map<String, Object> map);

    int updateLzCustomer(@Param("map") Map<String, Object> map);

    int deleteLzCustomer(@Param("id") Long id);
}
