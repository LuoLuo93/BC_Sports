package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "NxcrmMemberTagVO", description = "南讯会员标签记录")
public class NxcrmMemberTagVO {

    private String id;

    @ApiModelProperty("nasOuid")
    private String nasOuid;

    @ApiModelProperty("标签编码")
    private String tagCode;

    @ApiModelProperty("标签值编码")
    private String tagValueCode;

    @ApiModelProperty("标签值")
    private String tagValue;

    @ApiModelProperty("店铺ID")
    private Long shopId;

    @ApiModelProperty("店铺名称")
    private String shopName;

    @ApiModelProperty("状态：0-待处理，1-成功，3-失败")
    private Integer status;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @ApiModelProperty("批次号")
    private String batchNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
