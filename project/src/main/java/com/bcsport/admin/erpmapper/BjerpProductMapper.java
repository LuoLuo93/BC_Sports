package com.bcsport.admin.erpmapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BjerpProductMapper {
    long countProducts(@Param("materialNumber") String materialNumber, @Param("styleNumber") String styleNumber, @Param("materialName") String materialName, @Param("brandId") String brandId, @Param("kindId") String kindId);

    List<Map<String, Object>> searchProducts(@Param("materialNumber") String materialNumber, @Param("styleNumber") String styleNumber, @Param("materialName") String materialName, @Param("brandId") String brandId, @Param("kindId") String kindId, @Param("offset") long offset, @Param("pageSize") long pageSize);

    Map<String, Object> getProductByEan13(@Param("ean13") String ean13);

    /** 按货号(name)查询单个货品完整信息（贴纸资料详情页用），字段与 searchProducts 对齐 */
    Map<String, Object> getProductByMaterialNumber(@Param("materialNumber") String materialNumber);

    List<Map<String, Object>> getBrands();

    List<Map<String, Object>> getKinds();

    /** 按 productId 查询该商品每个尺码对应的条码(M_PRODUCT_ALIAS) */
    List<Map<String, Object>> getProductSizes(@Param("productId") String productId);

    /**
     * 按货号(name)更新 M_PRODUCT 的可编辑字段（执行标准/EAN13/4个材质字段/矫正尺码组ID/安全类别）。
     * 用于「贴纸资料维护」详情页保存。基本信息（货号/品名/品牌/价格等）不在此更新，避免侵入 ERP 主数据。
     * 矫正尺码组ID 复用 BOX_QTY_NEW 列存储。
     * @return 受影响行数（0=货号不存在）
     */
    int updateEditableFields(@Param("materialNumber") String materialNumber,
                             @Param("executionStandard") String executionStandard,
                             @Param("ean13") String ean13,
                             @Param("fabCode") String fabCode,
                             @Param("fabElement") String fabElement,
                             @Param("acCode") String acCode,
                             @Param("accElement") String accElement,
                             @Param("sizeGroupId") String sizeGroupId,
                             @Param("safetyCategory") String safetyCategory);

    // ==================== 预估成本管理 ====================

    /** 预估成本管理 - 计数 */
    long countEstimatedCost(@Param("materialNumber") String materialNumber,
                            @Param("styleNumber") String styleNumber,
                            @Param("materialName") String materialName);

    /** 预估成本管理 - 分页查询（物料编号/物料名称/款号/预估成本） */
    List<Map<String, Object>> searchEstimatedCost(@Param("materialNumber") String materialNumber,
                                                   @Param("styleNumber") String styleNumber,
                                                   @Param("materialName") String materialName,
                                                   @Param("offset") long offset,
                                                   @Param("pageSize") long pageSize);

    /**
     * 按物料编号(name)更新预估成本(PRECOST)
     * @return 受影响行数（0=物料编号不存在）
     */
    int updatePrecost(@Param("materialNumber") String materialNumber,
                      @Param("precost") String precost);

    /**
     * 批量查询存在的货号(导入前校验用)
     * @param materialNumbers 货号列表
     * @return 数据库中存在的货号集合(name)
     */
    List<String> selectExistMaterialNumbers(@Param("list") List<String> materialNumbers);

    /**
     * 批量更新预估成本(导入用),基于 Oracle MERGE INTO 单 SQL 更新多行。
     * 调用方需自行分批(每批建议 ≤500 条,避免 SQL 过长/绑定变量超限)。
     * @param rows 每行包含 materialNumber / precost 两个键
     * @return 受影响行数(MERGE 的 UPDATE 行数)
     */
    int batchUpdatePrecost(@Param("list") List<Map<String, String>> rows);
}
