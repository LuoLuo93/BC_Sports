package com.bcsport.admin.dto.ihr;

import lombok.Data;

/**
 * 员工基本信息 DTO
 */
@Data
public class IhrEmployeeBasicDTO {
    private String staffId;
    private String staffNo;
    private String staffName;
    private String nickName;
    private String mobileNo;
    private String email;
    private String staffStatus;
    private String idCardType;
    private String idCardNo;
    private String departmentName;
    private String positionName;
    private String sex;
    private String staffType;
    private String marryStatus;
    private String highestEducation;
    private String workPlace;
    private String birthday;
    private String contractBeginDate;
    private String contractEndDate;
    private String enrollInDate;
    private String probationEndDate;
    private String createdDate;
    private String lastUpdateDate;
}
