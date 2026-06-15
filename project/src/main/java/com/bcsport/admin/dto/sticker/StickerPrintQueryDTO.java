package com.bcsport.admin.dto.sticker;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class StickerPrintQueryDTO {

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("查看全部（默认仅自己）")
    private Boolean viewAll;

    @ApiModelProperty("申请单号")
    private String orderNo;

    @ApiModelProperty("申请人")
    private String applicant;

    @ApiModelProperty("申请日期-开始")
    private String startDate;

    @ApiModelProperty("申请日期-结束")
    private String endDate;
}
