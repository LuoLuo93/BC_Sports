package com.bcsport.admin.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("print_task")
public class PrintTask {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("task_id")
    private String taskId;

    @TableField("order_no")
    private String orderNo;

    @TableField("order_id")
    private String orderId;

    @TableField("material_number")
    private String materialNumber;

    @TableField("material_name")
    private String materialName;

    @TableField("style_number")
    private String styleNumber;

    @TableField("color")
    private String color;

    @TableField("brand_name")
    private String brandName;

    @TableField("kind_name")
    private String kindName;

    @TableField("size_name")
    private String sizeName;

    /** 修正尺码(矫正尺码)，空则前端回退展示 sizeName */
    @TableField("local_size_name")
    private String localSizeName;

    @TableField("print_qty")
    private Integer printQty;

    @TableField("template_file")
    private String templateFile;

    @TableField("printer_name")
    private String printerName;

    @TableField("print_data")
    private String printData;

    @TableField("agent_id")
    private String agentId;

    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("print_time")
    private LocalDateTime printTime;

    @TableField("dispatch_time")
    private LocalDateTime dispatchTime;

    @TableField("retry_count")
    private Integer retryCount;

    /** 补打来源任务ID；NULL 表示原始任务 */
    @TableField("source_task_id")
    private String sourceTaskId;

    /** 是否补打任务：0 否 / 1 是 */
    @TableField("is_reprint")
    private Integer isReprint;

    /** 补打原因（审计） */
    @TableField("reprint_reason")
    private String reprintReason;

    /** 打印批次ID：同一次下发(含补打)的任务共享，用于区分同一批次 */
    @TableField("batch_id")
    private String batchId;
}
