package com.bcsport.admin.entity.ihr;

import lombok.Data;

import java.util.Date;

/**
 * EZR 会员基础信息（来源：NXVipInfo + YZMobile 关联）
 */
@Data
public class EzrVipInfo {

    private String customerName;
    private String birthday;
    private Date cardReceiveTime;
    private Date sgRecruitTime;
    private String bindMobile;
    private String city;
    private Integer sex;
    private String unionid;
    private Integer cardReceivePlatform;
    private Integer cardReceiveShopType;
    private Integer platform;
    private Integer sgRecruitState;
    private String outSgExclusiveShopId;
    private String outSgExclusiveGuideId;
    private String grade;
}
