package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 企微群聊查询传输对象
 */
@Data
public class QywxGroupChatQueryDTO {

    /**
     * 群名称（模糊搜索）
     */
    private String name;

    /**
     * 群主姓名（模糊搜索）
     */
    private String ownerName;
}
