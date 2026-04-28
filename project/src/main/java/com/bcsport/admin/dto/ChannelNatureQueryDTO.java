package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 渠道性质查询传输对象
 */
@Data
public class ChannelNatureQueryDTO {
    private String natureName;
    private String natureCode;
    private String parentId;
    private Integer status;
    private String level1Id;
    private String level2Id;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
