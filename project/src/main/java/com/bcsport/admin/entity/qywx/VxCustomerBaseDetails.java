package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 企业微信群详情
 */
@Data
@TableName("VX_CustomerBaseDetails")
public class VxCustomerBaseDetails {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String chatId;

    private String name;

    private String owner;

    private String createTime;

    private Integer memberList;

    private java.time.LocalDateTime createTime2;
}
