package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "NxcrmTagInfoVO", description = "南讯标签信息")
public class NxcrmTagInfoVO {

    @ApiModelProperty("标签编码")
    private String tagCode;

    @ApiModelProperty("标签名称")
    private String tagName;

    @ApiModelProperty("所属文件夹ID")
    private String tagFolderId;

    @ApiModelProperty("父文件夹ID")
    private String parentFolderId;

    @ApiModelProperty("所属文件夹名称")
    private String tagFolderName;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("是否有值：0-否，1-是")
    private Integer hasValue;

    @ApiModelProperty("值类型")
    private String valueDataType;

    @ApiModelProperty("排序号")
    private Integer displayOrder;

    @ApiModelProperty("标签值列表")
    private List<NxcrmTagValueVO> tagValueList;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("同步时间")
    private LocalDateTime syncTime;
}
