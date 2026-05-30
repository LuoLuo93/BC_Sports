package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("bc_sports_sys_log")
public class SysLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String module;

    private String operation;

    private String method;

    private String params;

    private String ip;

    private String userId;

    private String username;

    private LocalDateTime operationTime;

    /** 1-成功, 0-失败 */
    private Integer status;

    private String errorMsg;
}
