package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("nanXOrderDetail")
public class NanXOrderDetail {

    @TableField("outOrderId")
    private String outOrderId;

    @TableField("outTradeId")
    private String outTradeId;

    @TableField("title")
    private String title;

    @TableField("outItemId")
    private String outItemId;

    @TableField("outerSkuId")
    private String outerSkuId;

    @TableField("orderNum")
    private BigDecimal orderNum;

    @TableField("orderTotalFee")
    private BigDecimal orderTotalFee;

    @TableField("orderPayment")
    private BigDecimal orderPayment;

    @TableField("orderPrice")
    private BigDecimal orderPrice;
}
