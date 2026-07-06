package com.bcsport.admin.dto;

import lombok.Data;

/**
 * HK ERP同步分页查询条件
 */
@Data
public class HkEmployeeQueryDTO {

    private String staffName;
    private String staffNo;
    private Integer syncStatus;
    private String startDate;
    private String endDate;
}
