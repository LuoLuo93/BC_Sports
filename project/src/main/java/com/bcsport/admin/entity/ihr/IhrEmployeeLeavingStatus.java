package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ihr_employee_leaving_status")
public class IhrEmployeeLeavingStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String employeeId;
    private String staffName;
    private String staffNo;
    private Integer syncStatus;
    private Date syncTime;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
}
