package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ERP人员同步日志
 * <p>
 * 与 erp_employee_sync_status(最新态，MERGE upsert) 不同，本表每次同步 INSERT 一条，保留完整历史。
 * 记录每次同步伯俊ERP的完整请求体和返回参数。
 */
@Data
@TableName("erp_employee_sync_log")
public class ErpEmployeeSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 同步类型: ONBOARDING / UPDATE / LEAVING */
    private String syncType;

    private String employeeId;
    private String staffName;
    private String staffNo;

    /** 同步状态: 1=成功, 2=失败, 3=已跳过 */
    private Integer syncStatus;

    /** 完整请求体(含sip_appkey/sip_sign/transactions) */
    private String requestBody;

    /** 伯俊返回的完整响应JSON */
    private String responseBody;

    private String errorMessage;

    private Date syncTime;
}
