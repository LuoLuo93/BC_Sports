package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象，用于向前端返回数据（已脱敏）
 */
@Data
public class UserVO {
    
    private String id;
    
    private String username;
    
    private String nickname;
    
    private String email;
    
    private String phone;
    
    private String avatar;
    
    private Integer status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    private String lastLoginIp;
    
    private String remark;
    
    private String deptId;
    
    private Integer sort;

    // 附加的业务名称展示字段
    private String roleNames;
    
    private String deptName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
