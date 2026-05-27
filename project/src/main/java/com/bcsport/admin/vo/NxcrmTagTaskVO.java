package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "NxcrmTagTaskVO", description = "南讯标签任务")
public class NxcrmTagTaskVO {

    private String id;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("外部店铺ID")
    private String outShopId;

    @ApiModelProperty("状态：0-待执行，1-执行中，2-已完成")
    private Integer status;

    @ApiModelProperty("总数")
    private Integer totalCount;

    @ApiModelProperty("成功数")
    private Integer successCount;

    @ApiModelProperty("失败数")
    private Integer failCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
