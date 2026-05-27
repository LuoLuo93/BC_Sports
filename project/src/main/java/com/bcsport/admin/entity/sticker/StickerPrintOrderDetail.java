package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sticker_print_order_detail")
public class StickerPrintOrderDetail {
    @TableId("id")
    private String id;
    @TableField("order_id")
    private String orderId;
    @TableField("article_no")
    private String articleNo;
    @TableField("article_name")
    private String articleName;
    @TableField("size_code")
    private String sizeCode;
    @TableField("size_name")
    private String sizeName;
    @TableField("size_group")
    private String sizeGroup;
    @TableField("ean13")
    private String ean13;
    @TableField("brand_name")
    private String brandName;
    @TableField("price")
    private BigDecimal price;
    @TableField("print_qty")
    private Integer printQty;
    @TableField("sort")
    private Integer sort;
    @TableField("create_time")
    private LocalDateTime createTime;
}
