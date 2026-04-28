package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 部门信息
 */
@Data
@TableName("department")
public class IhrDepartment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long departmentId;
    private String name;
    private Long parentId;
    private String type;
    private Date createTime;
    private Date updateTime;
}
