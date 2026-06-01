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
}
