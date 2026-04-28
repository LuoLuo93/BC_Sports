package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业微信外部联系人跟进信息
 */
@Data
@TableName("VX_CustomerListDetails_follow_info")
public class VxCustomerlistdetailsFollowInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String userid;

    private String remark;

    private String description;

    private String createtime;

    private String addWay;

    private String operUserid;

    private LocalDateTime createTime2;

    private String externalUserid;
}
