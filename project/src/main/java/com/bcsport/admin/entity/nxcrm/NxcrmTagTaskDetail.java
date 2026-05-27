package com.bcsport.admin.entity.nxcrm;

import com.baomidou.mybatisplus.annotation.*;
import com.bcsport.admin.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BC_SPORTS_NXCRM_TAG_TASK_DETAIL")
public class NxcrmTagTaskDetail extends BaseEntity {

    @TableField("TASK_ID")
    private String taskId;

    @TableField("MEMBER_ID")
    private Long memberId;

    @TableField("TAG_DATA_JSON")
    private String tagDataJson;

    @TableField("STATUS")
    private Integer status;

    @TableField("ERROR_MSG")
    private String errorMsg;
}
