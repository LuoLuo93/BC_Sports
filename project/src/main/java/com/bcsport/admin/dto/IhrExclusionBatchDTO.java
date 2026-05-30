
package com.bcsport.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel(value = "IhrExclusionBatchDTO", description = "IHR员工排除批量操作DTO")
public class IhrExclusionBatchDTO {

    @NotEmpty(message = "ID列表不能为空")
    @ApiModelProperty("操作的ID列表")
    private List<String> ids;

    @ApiModelProperty("批量操作的目标状态（0-禁用，1-启用），仅批量更新状态时需要")
    private Integer targetStatus;
}
