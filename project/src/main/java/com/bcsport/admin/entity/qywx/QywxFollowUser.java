package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业微信配置了客户联系功能的成员表
 */
@Data
@TableName("VX_FollowUserList")
public class QywxFollowUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 成员UserID
     */
    private String followUser;

    /**
     * 录入时间
     */
    private LocalDateTime createTime;
}
