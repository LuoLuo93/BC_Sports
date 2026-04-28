package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业微信外部联系人基本信息
 */
@Data
@TableName("VX_CustomerListDetails_external_contact")
public class VxCustomerlistdetailsExternalContact {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String externalUserid;

    private String name;

    private String type;

    private String gender;

    private String unionid;

    private String corpName;

    private LocalDateTime createTime;
}
