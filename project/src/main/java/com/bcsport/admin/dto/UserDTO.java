package com.bcsport.admin.dto;

import com.bcsport.admin.entity.User;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 用户数据传输对象（用于新增和修改） */
@Data
public class UserDTO {
    
    private String id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空", groups = User.Create.class)
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间", groups = User.Create.class)
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    
    private Integer status;
    
    private String remark;
    
    private String deptId;
    
    private Integer sort;

    /**
     * 角色ID列表
     */
    private List<String> roleIds;
}
