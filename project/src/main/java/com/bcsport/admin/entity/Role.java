package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_role")
public class Role extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String roleName;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    private String roleCode;

    /**
     * 角色描述
     */
    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    private String description;

    /**
     * 状态（0：禁用，1：启用）
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;
}
