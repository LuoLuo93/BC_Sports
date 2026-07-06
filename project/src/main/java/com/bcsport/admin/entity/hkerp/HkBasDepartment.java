package com.bcsport.admin.entity.hkerp;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * ERP部门表（只读，用于按部门名称查部门ID）
 * <p>
 * 对应源项目内联 SQL 查询的 Bas_DepartMent 表。
 */
@Data
@TableName("Bas_DepartMent")
public class HkBasDepartment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String departmentId;
    private String departmentName;
}
