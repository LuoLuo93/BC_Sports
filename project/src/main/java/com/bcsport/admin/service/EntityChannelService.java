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
}
