package com.bcsport.admin.dto.nxcrm;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NxcrmTagTaskDetailQueryDTO {

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("状态：0-待执行，1-成功，2-失败")
    private Integer status;
}
