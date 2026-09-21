package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 企微打标签记录查询传输对象
 */
@Data
public class QywxTagRecordQueryDTO {

    private String externalUserid;

    private String tagId;

    private String tagName;

    private String batchNo;

    /** 1=成功 0=接口失败 2=标签未匹配 3=客户未匹配 */
    private Integer status;
}
