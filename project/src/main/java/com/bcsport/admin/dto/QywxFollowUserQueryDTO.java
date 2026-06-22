package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 企微客户联系成员查询传输对象
 */
@Data
public class QywxFollowUserQueryDTO {

    /**
     * 成员姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 主部门名称
     */
    private String mainDepartment;
}
