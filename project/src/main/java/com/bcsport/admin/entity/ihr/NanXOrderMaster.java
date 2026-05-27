package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("nanXOrderMaster")
public class NanXOrderMaster {

    @TableField("outTradeId")
    private String outTradeId;

    @TableField("totalFee")
    private BigDecimal totalFee;

    @TableField("payment")
    private BigDecimal payment;

    @TableField("nasOuid")
    private String nasOuid;

    @TableField("created")
    private Date created;

    @TableField("num")
    private BigDecimal num;

    @TableField(exist = false)
    private List<NanXOrderDetail> details;
}
