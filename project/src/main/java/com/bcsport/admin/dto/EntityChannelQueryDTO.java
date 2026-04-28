package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 实体渠道配置查询DTO
 */
@Data
public class EntityChannelQueryDTO {

    private String entityType;

    private String entityName;

    private String externalId;

    private String brandId;

    private String channelTypeId;
    
    private String channelDefId;

    private String channelNatureId;

    private String businessTypeId;

    private String regionLevel1Id;

    private String regionLevel2Id;

    private Integer status;
}
