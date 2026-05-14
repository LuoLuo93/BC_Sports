package com.bcsport.admin.dto;

import lombok.Data;

@Data
public class ErpEmployeeQueryDTO {

    private String staffName;
    private String staffNo;
    private Integer syncStatus;
    private String startDate;
    private String endDate;
}
