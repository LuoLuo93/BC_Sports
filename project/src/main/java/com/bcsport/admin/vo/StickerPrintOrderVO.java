package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

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

        @ApiModelProperty("货品编码")
        private String productCode;

        @ApiModelProperty("货品名称")
        private String productName;

        @ApiModelProperty("规格")
        private String spec;

        @ApiModelProperty("数量")
        private Integer quantity;

        @ApiModelProperty("打印状态：0-未打印，1-已打印")
        private Integer printStatus;
    }
}
