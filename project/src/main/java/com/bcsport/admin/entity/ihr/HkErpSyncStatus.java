package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HK ERP直写链路同步状态
 * <p>
 * 表 hk_erp_sync_status（BC_SPORTS_IHR 库），与伯俊链路的 erp_employee_sync_status 隔离。
 * sync_type 取值：HK_ONBOARDING / HK_UPDATE / HK_LEAVING。
 */
@Data
@TableName("hk_erp_sync_status")
public class HkErpSyncStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 同步类型：HK_ONBOARDING / HK_UPDATE / HK_LEAVING */
    private String syncType;

    /** 关联 employee_information.id */
    private String employeeId;

    private String staffName;
    private String staffNo;

    /** 0待同步 1成功 2失败 3跳过 */
    private Integer syncStatus;

    private Date syncTime;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
}
