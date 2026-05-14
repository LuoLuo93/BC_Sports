package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_dict_type")
public class DictType extends BaseEntity {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100)
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100)
    private String dictType;

    private Integer status;

    private String remark;
}
