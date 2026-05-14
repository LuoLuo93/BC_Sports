package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_dict_data")
public class DictData extends BaseEntity {

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100)
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100)
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Size(max = 100)
    private String dictValue;

    private Integer sort;

    private Integer status;

    private String remark;
}
