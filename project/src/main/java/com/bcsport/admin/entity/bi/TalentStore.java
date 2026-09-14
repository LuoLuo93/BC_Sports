package com.bcsport.admin.entity.bi;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 达人店铺（对应 BC_SPORTS_BI_TALENT_STORE）
 * 达人名称 + 伯俊ERP店仓编码/名称 的绑定关系，手工表单维护
 */
@Data
@TableName("BC_SPORTS_BI_TALENT_STORE")
public class TalentStore implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 达人名称 */
    private String talentName;

    /** 伯俊ERP店仓编码（C_STORE.CODE） */
    private String storeCode;

    /** 伯俊ERP店仓名称（C_STORE.NAME） */
    private String storeName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
