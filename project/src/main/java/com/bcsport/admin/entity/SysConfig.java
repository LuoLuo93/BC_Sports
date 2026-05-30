package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bc_sports_sys_config")
public class SysConfig extends BaseEntity {

    private String configKey;

    private String configValue;

    private String configName;

    private String configGroup;

    private Integer sort;

    private String remark;
}
