package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ihr_employee_exclusion")
@ApiModel(value = "IhrEmployeeExclusion对象", description = "IHR员工排除名单")
public class IhrEmployeeExclusion implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty("员工姓名")
    @TableField("staff_name")
    private String staffName;

    @ApiModelProperty("工号")
    @TableField("staff_no")
    private String staffNo;

    @ApiModelProperty("排除类型：1-入职排除，2-离职排除")
    @TableField("exclusion_type")
    private Integer exclusionType;

    @ApiModelProperty("状态：0-禁用，1-启用")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("排除原因")
    @TableField("reason")
    private String reason;

    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty("创建人")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @ApiModelProperty("更新人")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
