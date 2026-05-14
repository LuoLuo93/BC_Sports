package com.bcsport.admin.dto;

import lombok.Data;

@Data
public class IhrEmployeeLeavingQueryDTO {

    private String staffName;
    private String staffNo;
    private Integer syncStatus;
    private String startDate;
    private String endDate;
}
