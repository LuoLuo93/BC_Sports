package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工基本信息
 */
@Data
@TableName("employees")
public class IhrEmployee implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

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
    private Date createTime;
    private Date updateTime;
}
