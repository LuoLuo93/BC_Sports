package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 企业微信群成员
 */
@Data
@TableName("VX_CustomerBaseDetails_GroupMembers")
public class VxCustomerBaseDetailsGroupMembers {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String chatId;

    private String userid;

    private String type;

    private String joinTime;

    private String joinScene;

    private String invitor;

    private String name;

    private String unionId;

    private java.time.LocalDateTime createTime;
}
