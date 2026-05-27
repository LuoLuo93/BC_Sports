package com.bcsport.admin.dto.nxcrm;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NxcrmMemberTagRecordQueryDTO {

    @ApiModelProperty("批次号")
    private String batchNo;

    @ApiModelProperty("状态：0-待处理，1-成功，3-失败")
    private Integer status;
}
