package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("erp_employee_sync_status")
public class ErpEmployeeSyncStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String syncType;
    private String employeeId;
    private String staffName;
    private String staffNo;
    private Integer syncStatus;
    private Long erpObjectId;      // 伯俊ERP返回的objectid，用于后续修改
    private Date syncTime;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
}
