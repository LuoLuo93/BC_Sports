package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业微信客户联系成员部门关系表
 */
@Data
@TableName("VX_CustomerList_department")
public class QywxCustomerListDepartment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 成员UserID
     */
    private String userid;

    /**
     * 部门ID
     */
    private Integer department;

    /**
     * 录入时间
     */
    private LocalDateTime createTime;
}
