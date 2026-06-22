package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 企微群聊统计查询传输对象
 */
@Data
public class QywxGroupStatQueryDTO {

    /**
     * 群主UserID（必填）
     */
    private String owner;
}
