package com.bcsport.admin.entity.bi;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 货品资料导入日志（对应 BC_SPORTS_GOODS_IMPORT_LOG）
 */
@Data
@TableName("BC_SPORTS_GOODS_IMPORT_LOG")
public class GoodsImportLog implements Serializable {

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
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;
}
