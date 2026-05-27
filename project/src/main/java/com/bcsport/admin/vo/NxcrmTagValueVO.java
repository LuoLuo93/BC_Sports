package com.bcsport.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "NxcrmTagValueVO", description = "南讯标签值")
public class NxcrmTagValueVO {

    private Long id;

    @ApiModelProperty("标签编码")
    private String tagCode;

    @ApiModelProperty("标签值编码")
    private String tagValueCode;

    @ApiModelProperty("标签值名称")
    private String tagValueName;

    @ApiModelProperty("排序号")
    private Integer displayOrder;
}
