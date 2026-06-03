package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BjerpStoreMapper {

    long countStores(@Param("code") String code, @Param("name") String name);

    List<Map<String, Object>> searchStores(@Param("code") String code, @Param("name") String name,
                                           @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<Map<String, Object>> listAllStores();
}
