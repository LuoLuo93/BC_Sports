package com.bcsport.admin.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码修改DTO
 */
@Data
public class PasswordDTO {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    private String oldPassword;
}
