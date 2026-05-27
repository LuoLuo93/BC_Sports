package com.bcsport.admin.dto.sticker;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class StickerPrintQueryDTO {

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("查看全部（默认仅自己）")
    private Boolean viewAll;
}
