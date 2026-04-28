package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 企业微信群列表（临时表）
 */
@Data
@TableName("VX_CustomerBase")
public class VxCustomerBase {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String chatId;

    private String status;

    private java.time.LocalDateTime createTime;
}
