package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 地区查询传输对象
 */
@Data
public class RegionQueryDTO {
    private String regionName;
    private String regionCode;
    private String parentId;
    private Integer status;
    private String level1Id;
    private String level2Id;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
