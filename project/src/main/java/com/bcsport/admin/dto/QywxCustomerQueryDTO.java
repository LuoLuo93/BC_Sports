package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 企微成员客户查询传输对象
 */
@Data
public class QywxCustomerQueryDTO {

    /**
     * 成员UserID（必填）
     */
    private String userid;

    /**
     * 客户名称（模糊搜索）
     */
    private String name;
}
