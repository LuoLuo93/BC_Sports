package com.bcsport.admin.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 实体渠道配置DTO
 */
@Data
public class EntityChannelDTO {

    private String id;

    @NotBlank(message = "外部实体ID不能为空")
    private String externalId;

    private String brandId;

    @NotBlank(message = "实体类型不能为空")
    private String entityType;

    @NotBlank(message = "实体名称不能为空")
    private String entityName;

    private String channelTypeId;

    private String channelDefId;

    private String channelNatureId;

    private String businessTypeId;

    private String regionLevel1Id;

    private String regionLevel2Id;

    private Integer status = 1;

    private Integer sort = 0;
}
