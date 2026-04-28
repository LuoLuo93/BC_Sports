package com.bcsport.admin.vo;

import lombok.Data;

/**
 * 实体渠道配置VO
 */
@Data
public class EntityChannelVO {

    private String id;

    private String externalId;

    private String brandId;

    private String brandName;

    private String entityType;

    private String entityTypeName;

    private String entityName;

    private String channelTypeId;

    private String channelTypeName;

    private String channelDefId;

    private String channelDefName;

    private String channelNatureId;

    private String channelNatureName;

    private String businessTypeId;

    private String businessTypeName;

    private String regionLevel1Id;

    private String regionLevel1Name;

    private String regionLevel2Id;

    private String regionLevel2Name;

    private Integer status;

    private String statusName;

    private Integer sort;
}
