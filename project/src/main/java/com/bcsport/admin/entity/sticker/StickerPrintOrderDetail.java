package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import com.bcsport.admin.common.FieldLabel;
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

    @TableField("product_id")
    private String productId;

    @FieldLabel("条码")
    @TableField("barcode")
    private String barcode;

    @FieldLabel("款号")
    @TableField("style_number")
    private String styleNumber;

    @FieldLabel("物料名称")
    @TableField("material_name")
    private String materialName;

    @FieldLabel("物料号/货号")
    @TableField("material_number")
    private String materialNumber;

    @FieldLabel("颜色")
    @TableField("color")
    private String color;

    @FieldLabel("执行标准")
    @TableField("execution_standard")
    private String executionStandard;

    @FieldLabel("尺码代码")
    @TableField("size_code")
    private String sizeCode;

    @FieldLabel("尺码名称")
    @TableField("size_name")
    private String sizeName;

    @FieldLabel("尺码组")
    @TableField("size_group")
    private String sizeGroup;

    @FieldLabel("EAN13条码")
    @TableField("ean13")
    private String ean13;

    @FieldLabel("品牌名称")
    @TableField("brand_name")
    private String brandName;

    @TableField("brand_id")
    private String brandId;

    @TableField("kind_id")
    private String kindId;

    @FieldLabel("类别名称")
    @TableField("kind_name")
    private String kindName;

    @FieldLabel("价格")
    @TableField("price")
    private BigDecimal price;

    @FieldLabel("打印数量")
    @TableField("print_qty")
    private Integer printQty;

    @TableField("sort")
    private Integer sort;

    @FieldLabel("产地")
    @TableField("origin")
    private String origin;

    @FieldLabel("生产商")
    @TableField("manufacturer")
    private String manufacturer;

    @FieldLabel("生产商地址")
    @TableField("manufacturer_address")
    private String manufacturerAddress;

    @FieldLabel("联系电话")
    @TableField("contact_phone")
    private String contactPhone;

    @FieldLabel("材质成分")
    @TableField("material_composition")
    private String materialComposition;

    @TableField("create_time")
    private LocalDateTime createTime;

    @FieldLabel("本地尺码组ID")
    @TableField("local_group_id")
    private String localGroupId;

    @FieldLabel("本地尺码组名称")
    @TableField("local_group_name")
    private String localGroupName;

    @FieldLabel("本地尺码ID")
    @TableField("local_size_id")
    private String localSizeId;

    @FieldLabel("本地尺码名称")
    @TableField("local_size_name")
    private String localSizeName;
}
