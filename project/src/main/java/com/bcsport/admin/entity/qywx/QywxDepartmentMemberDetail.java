package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业微信部门成员详情表
 */
@Data
@TableName("VX_DepartmentMembersList")
public class QywxDepartmentMemberDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 成员UserID
     */
    private String userid;

    /**
     * 成员名称
     */
    private String name;

    /**
     * 全局唯一
     */
    private String openUserid;

    /**
     * 主部门
     */
    private String mainDepartment;

    /**
     * 职位
     */
    private String position;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 状态
     */
    private String status;

    /**
     * 录入时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
