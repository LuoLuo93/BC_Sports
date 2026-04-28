package com.bcsport.admin.entity.qywx;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业微信成员扩展属性表
 * 对应原接口项目中的 VX_attrsBase
 */
@Data
public class QywxAttrsBase {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 成员UserID
     */
    private String userid;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 属性值（对于入职日期就是日期字符串）
     */
    private String enrollInDate;

    /**
     * 属性类型
     */
    private String type;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
