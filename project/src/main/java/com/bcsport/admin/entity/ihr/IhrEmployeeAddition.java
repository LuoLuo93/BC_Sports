package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 每日新增员工ID表
 */
@Data
@TableName("employee_additions")
public class IhrEmployeeAddition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String employeesId;
    private Date syncDate;
    private Date createTime;
}
