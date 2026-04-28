package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企业微信部门成员
 */
@Data
@TableName("VX_DepartmentMember")
public class QywxDepartmentMember implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String userid;
    private String name;
    private String openUserid;
    private Integer department;
    private Date createTime;
    private Date updateTime;
}
