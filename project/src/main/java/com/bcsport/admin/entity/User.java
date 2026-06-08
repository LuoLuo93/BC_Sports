package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_user")
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 校验分组：新增
     */
    public interface Create extends javax.validation.groups.Default {
    }

    /**
     * 校验分组：更新
     */
    public interface Update extends javax.validation.groups.Default {
    }

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空", groups = Create.class)
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间", groups = Create.class)
    private String password;

    /**
     * BCrypt 加密密码（用于渐进式迁移）
     */
    private String passwordNew;

    /**
     * 盐
     */
    private String salt;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态（0：禁用，1：启用）
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 备注
     */
    private String remark;

    /**
     * 部门ID
     */
    private String deptId;

    /**
     * 排序权重
     */
    private Integer sort;

    /**
     * 角色名称列表（逗号分隔），非数据库字段
     */
    @TableField(exist = false)
    private String roleNames;

    /**
     * 部门名称，非数据库字段
     */
    @TableField(exist = false)
    private String deptName;
}
