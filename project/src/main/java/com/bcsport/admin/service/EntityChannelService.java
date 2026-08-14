package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.EntityChannelDTO;
import com.bcsport.admin.dto.EntityChannelQueryDTO;
import com.bcsport.admin.vo.EntityChannelVO;

/**
 * 实体渠道配置服务接口
 */
public interface EntityChannelService {

    /**
     * 分页查询实体渠道配置列表
     */
    PageResult<EntityChannelVO> pageEntityChannels(PageQuery pageQuery, EntityChannelQueryDTO queryDTO);

    /**
     * 根据ID查询实体渠道配置
     */
    EntityChannelVO getEntityChannelVOById(String id);

    /**
     * 新增实体渠道配置
     */
    boolean addEntityChannel(EntityChannelDTO dto);

    /**
     * 修改实体渠道配置（只允许修改渠道属性）
     */
    boolean updateEntityChannel(EntityChannelDTO dto);

    /**
     * 删除实体渠道配置（逻辑删除完
     */
    boolean deleteEntityChannel(String id);

    /**
     * 按实体查询所有渠道配置
     */
    java.util.List<EntityChannelVO> listByEntity(String externalId, String entityType);

    /**
     * 批量保存实体渠道配置（更新已有、新增新条目、删除移除的）
     */
    boolean batchSave(String externalId, String entityType, java.util.List<EntityChannelDTO> list);

    /**
     * 从Excel导入实体渠道配置（名称自动解析为ID）
     */
    java.util.Map<String, Object> importFromExcel(org.springframework.web.multipart.MultipartFile file) throws Exception;

    /**
     * 同步本地店仓名称(entity_name)为伯俊 ERP C_STORE 的最新名称。
     * 仅更新 ERP 中能查到的编码对应记录，ERP 查不到的保留原名。
     * @return 统计结果：total/synced/unchanged/notInErp
     */
    java.util.Map<String, Object> syncStoreNames();

    /**
     * 从数仓(销售+库存)同步店铺+品牌到实体渠道配置。
     * 数仓出现但本地缺失的 (external_id, brand_id) 自动新增；品牌名匹配不到本地品牌的跳过。
     * @return 统计结果：total/inserted/revived/existing/brandUnmatched/unmatchedBrands
     */
    java.util.Map<String, Object> syncStoreBrands();
}
