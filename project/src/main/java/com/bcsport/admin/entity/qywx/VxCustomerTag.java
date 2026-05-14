package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("VX_CustomerTag")
public class VxCustomerTag {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("externalUserid")
    private String externalUserid;

    private String userid;

    @TableField("tagId")
    private String tagId;

    @TableField("tagName")
    private String tagName;

    private String source;

    @TableField("batchNo")
    private String batchNo;

    @TableField("createTime")
    private LocalDateTime createTime;
}
