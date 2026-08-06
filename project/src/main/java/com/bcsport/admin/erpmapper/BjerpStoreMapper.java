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

    /** 渠道配置表单专用精简查询，只返回 CODE/NAME（字段名与前端 EntityChannelForm 绑定一致） */
    List<Map<String, Object>> searchStoresSimple(@Param("code") String code, @Param("name") String name,
                                                  @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<Map<String, Object>> listAllStores();

    /** 店仓主品牌下拉（C_STOREATTRIBVALUE DIM5） */
    List<Map<String, Object>> listBrands();

    /** 店仓零售督导下拉（C_STOREATTRIBVALUE DIM6） */
    List<Map<String, Object>> listSupervisors();

    /**
     * 编辑店仓品牌/督导归属，写回 ERP C_STORE。
     * @return 受影响行数（0=店仓ID不存在）
     */
    int updateStoreAttrib(@Param("storeId") String storeId,
                          @Param("brandId") String brandId,
                          @Param("supervisorId") String supervisorId);
}
