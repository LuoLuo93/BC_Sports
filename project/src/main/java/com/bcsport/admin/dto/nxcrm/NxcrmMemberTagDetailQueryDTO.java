package com.bcsport.admin.dto.nxcrm;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NxcrmMemberTagDetailQueryDTO {

    @ApiModelProperty("nasOuid")
    private String nasOuid;

    @ApiModelProperty("标签名称")
    private String tagName;
}
