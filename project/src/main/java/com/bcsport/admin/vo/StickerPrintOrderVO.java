package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "StickerPrintOrderVO", description = "贴纸打印单")
public class StickerPrintOrderVO {

    private String id;

    @ApiModelProperty("单据编号")
    private String orderNo;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("申请人")
    private String applicant;

    @ApiModelProperty("审核人")
    private String reviewer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("审核时间")
    private LocalDateTime reviewTime;

    @ApiModelProperty("审核备注")
    private String reviewRemark;

    @ApiModelProperty("备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("打印时间")
    private LocalDateTime printTime;

    @ApiModelProperty("打印人")
    private String printBy;

    @ApiModelProperty("明细列表")
    private List<StickerPrintOrderDetailVO> details;

    @Data
    @ApiModel(value = "StickerPrintOrderDetailVO", description = "贴纸打印单明细")
    public static class StickerPrintOrderDetailVO {

        private String id;

        @ApiModelProperty("伯俊商品ID")
        private String productId;

        @ApiModelProperty("货号")
        private String materialNumber;

        @ApiModelProperty("货品名称")
        private String materialName;

        @ApiModelProperty("款号")
        private String styleNumber;

        @ApiModelProperty("颜色")
        private String color;

        @ApiModelProperty("执行标准")
        private String executionStandard;

        @ApiModelProperty("尺码编码")
        private String sizeCode;

        @ApiModelProperty("尺码名称")
        private String sizeName;

        @ApiModelProperty("尺码组")
        private String sizeGroup;

        @ApiModelProperty("EAN13")
        private String ean13;

        @ApiModelProperty("条码(每尺码对应)")
        private String barcode;

        @ApiModelProperty("品牌名称")
        private String brandName;

        @ApiModelProperty("品牌ID")
        private String brandId;

        @ApiModelProperty("类别ID")
        private String kindId;

        @ApiModelProperty("类别名称")
        private String kindName;

        @ApiModelProperty("价格")
        private BigDecimal price;

        @ApiModelProperty("打印数量")
        private Integer printQty;

        @ApiModelProperty("排序")
        private Integer sort;

        @ApiModelProperty("产地")
        private String origin;

        @ApiModelProperty("制造商")
        private String manufacturer;

        @ApiModelProperty("制造商地址")
        private String manufacturerAddress;

        @ApiModelProperty("联系电话")
        private String contactPhone;

        @ApiModelProperty("面料编码")
        private String fabCode;

        @ApiModelProperty("面料成分")
        private String fabElement;

        @ApiModelProperty("辅料编码")
        private String acCode;

        @ApiModelProperty("辅料成分")
        private String accElement;

        @ApiModelProperty("本地尺码组ID")
        private String localGroupId;

        @ApiModelProperty("本地尺码组名称")
        private String localGroupName;

        @ApiModelProperty("本地尺码ID")
        private String localSizeId;

        @ApiModelProperty("本地尺码名称")
        private String localSizeName;
    }
}
