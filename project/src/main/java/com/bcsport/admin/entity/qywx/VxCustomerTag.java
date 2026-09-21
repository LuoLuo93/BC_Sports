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

    /** 1=成功 0=企微接口失败 2=标签未匹配 3=客户未匹配 */
    @TableField("status")
    private Integer status;

    /** 失败/未匹配原因 */
    @TableField("errmsg")
    private String errmsg;

    /** ADD=打标 REMOVE=移除标签 */
    @TableField("tagAction")
    private String tagAction;

    @TableField("createTime")
    private LocalDateTime createTime;
}
