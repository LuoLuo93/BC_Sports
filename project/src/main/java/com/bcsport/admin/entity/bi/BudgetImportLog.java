package com.bcsport.admin.entity.bi;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺日预算导入日志（对应 BC_SPORTS_BUDGET_LOG）
 * 走主库 dataSource(BC_SPORTS schema)，与 GoodsImportLog 同构
 */
@Data
@TableName("BC_SPORTS_BUDGET_LOG")
public class BudgetImportLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String fileName;
    private Long fileSize;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;

    /** SUCCESS / PARTIAL / FAILED */
    private String status;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;
}
