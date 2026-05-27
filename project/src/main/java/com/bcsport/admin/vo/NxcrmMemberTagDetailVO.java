package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "NxcrmMemberTagDetailVO", description = "南讯会员标签详情")
public class NxcrmMemberTagDetailVO {

    private Long id;

    @ApiModelProperty("nasOuid")
    private String nasOuid;

    @ApiModelProperty("标签名称")
    private String tagName;

    @ApiModelProperty("标签值名称")
    private String tagValueName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("同步时间")
    private LocalDateTime syncTime;
}
