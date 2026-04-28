package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企业微信部门
 */
@Data
@TableName("VX_DepartmentList")
public class QywxDepartment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String departId;
    private String name;
    private String nameEn;
    private String parentId;
    private String parentOrder;
    private Date createTime;
    private Date updateTime;
}
