package com.bcsport.admin.entity.nxcrm;

import com.baomidou.mybatisplus.annotation.*;
import com.bcsport.admin.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BC_SPORTS_NXCRM_TAG_TASK")
public class NxcrmTagTask extends BaseEntity {

    @TableField("TASK_NAME")
    private String taskName;

    @TableField("OUT_SHOP_ID")
    private String outShopId;

    @TableField("STATUS")
    private Integer status;

    @TableField("TOTAL_COUNT")
    private Integer totalCount;

    @TableField("SUCCESS_COUNT")
    private Integer successCount;

    @TableField("FAIL_COUNT")
    private Integer failCount;
}
