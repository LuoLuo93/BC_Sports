package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BjerpCustomerMapper {

    long countCustomers(@Param("code") String code, @Param("name") String name);

    List<Map<String, Object>> searchCustomers(@Param("code") String code, @Param("name") String name,
                                              @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<Map<String, Object>> listAllCustomers();
}
