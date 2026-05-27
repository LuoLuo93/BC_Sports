package com.bcsport.admin.dto.nxcrm;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NxcrmTagListQueryDTO {

    @ApiModelProperty("关键词（标签名/编码/文件夹）")
    private String keyword;
}
