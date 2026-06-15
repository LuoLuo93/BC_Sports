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
    @TableField("style_number")
    private String styleNumber;
    @TableField("material_name")
    private String materialName;
    @TableField("material_number")
    private String materialNumber;
    @TableField("color")
    private String color;
    @TableField("execution_standard")
    private String executionStandard;
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
    @TableField("kind_id")
    private String kindId;
    @TableField("kind_name")
    private String kindName;
    @TableField("price")
    private BigDecimal price;
    @TableField("print_qty")
    private Integer printQty;
    @TableField("sort")
    private Integer sort;
    @TableField("origin")
    private String origin;
    @TableField("manufacturer")
    private String manufacturer;
    @TableField("manufacturer_address")
    private String manufacturerAddress;
    @TableField("contact_phone")
    private String contactPhone;
    @TableField("material_composition")
    private String materialComposition;
    @TableField("create_time")
    private LocalDateTime createTime;
}
