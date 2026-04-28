package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 渠道类型查询传输对象
 */
@Data
public class ChannelTypeQueryDTO {
    private String typeName;
    private String typeCode;
    private String parentId;
    private Integer status;
    private String level1Id;
    private String level2Id;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
