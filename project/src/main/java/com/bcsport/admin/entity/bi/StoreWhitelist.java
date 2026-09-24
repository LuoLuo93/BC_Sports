package com.bcsport.admin.entity.bi;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺白名单（对应 BC_SPORTS_BI_STORE_WHITELIST）
 * 伯俊ERP店仓编码/名称，手工表单维护；
 * 反向白名单语义：数仓 SP_FILL_ODS_SALES_MAIN ④小程序更新时，订单当前 STORE_CODE
 * 命中本表则不改写店铺两列，只更新 SALES_TYPE/VIP_MOBILE/营业员
 */
@Data
@TableName("BC_SPORTS_BI_STORE_WHITELIST")
public class StoreWhitelist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 伯俊ERP店仓编码（C_STORE.CODE），未删除记录内唯一 */
    private String storeCode;

    /** 伯俊ERP店仓名称（C_STORE.NAME） */
    private String storeName;

    /** 数据来源：MANUAL 手工录入 / AUTO 伯俊定时同步(C_STORE自提属性) */
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_AUTO = "AUTO";

    private String source;

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
