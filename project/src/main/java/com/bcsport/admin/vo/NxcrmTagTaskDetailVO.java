package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "NxcrmTagTaskDetailVO", description = "南讯标签任务明细")
public class NxcrmTagTaskDetailVO {

    private String id;

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("会员ID")
    private Long memberId;

    @ApiModelProperty("标签数据JSON")
    private String tagDataJson;

    @ApiModelProperty("状态：0-待执行，1-成功，2-失败")
    private Integer status;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
