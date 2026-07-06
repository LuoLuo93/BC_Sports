package com.bcsport.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 旧版HK ERP 同步统计结果
 */
@Data
@ApiModel("HK ERP同步统计")
public class HkSyncStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("入职-新增人数")
    private int onboarded;
    @ApiModelProperty("入职-二次入职人数")
    private int reOnboarded;
    @ApiModelProperty("入职-跳过(已存在)人数")
    private int skipped;
    @ApiModelProperty("入职-失败人数")
    private int failed;

    @ApiModelProperty("变更-手机号修改人数")
    private int phoneUpdated;
    @ApiModelProperty("变更-部门修改人数")
    private int shopUpdated;

    @ApiModelProperty("离职-禁用人数")
    private int leaveDisabled;
    @ApiModelProperty("离职-满30天转离职人数")
    private int leaveFinalized;

    @ApiModelProperty("处理失败异常条数")
    private int errorCount;

    @ApiModelProperty("描述信息")
    private String message;

    public void incrOnboarded() { this.onboarded++; }
    public void incrReOnboarded() { this.reOnboarded++; }
    public void incrSkipped() { this.skipped++; }
    public void incrFailed() { this.failed++; }
    public void incrPhoneUpdated() { this.phoneUpdated++; }
    public void incrShopUpdated() { this.shopUpdated++; }
    public void incrLeaveDisabled() { this.leaveDisabled++; }
    public void incrLeaveFinalized() { this.leaveFinalized++; }
    public void incrError() { this.errorCount++; }
}
