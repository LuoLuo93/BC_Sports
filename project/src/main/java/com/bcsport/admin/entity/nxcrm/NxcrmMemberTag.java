package com.bcsport.admin.entity.nxcrm;

import com.baomidou.mybatisplus.annotation.*;
import com.bcsport.admin.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BC_SPORTS_NXCRM_MEMBER_TAG")
public class NxcrmMemberTag extends BaseEntity {

    @TableField("NAS_OUID")
    private String nasOuid;

    @TableField("TAG_CODE")
    private String tagCode;

    @TableField("TAG_VALUE_CODE")
    private String tagValueCode;

    @TableField("TAG_VALUE")
    private String tagValue;

    @TableField("SHOP_ID")
    private Long shopId;

    @TableField("SHOP_NAME")
    private String shopName;

    @TableField("STATUS")
    private Integer status;

    @TableField("ERROR_MSG")
    private String errorMsg;

    @TableField("BATCH_NO")
    private String batchNo;
}
