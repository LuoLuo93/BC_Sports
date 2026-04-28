
package com.bcsport.admin.dto;

import lombok.Data;

@Data
public class IhrEmployeeExclusionQueryDTO {

    private String staffName;

    private String staffNo;

    private Integer exclusionType;

    private Integer status;
}

