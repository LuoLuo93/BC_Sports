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
}
