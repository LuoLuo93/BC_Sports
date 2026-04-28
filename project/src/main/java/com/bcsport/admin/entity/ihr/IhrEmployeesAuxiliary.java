package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工ID辅助表 */
@Data
@TableName("employees_auxiliary")
public class IhrEmployeesAuxiliary implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String employeesId;
    private Date createTime;
}
