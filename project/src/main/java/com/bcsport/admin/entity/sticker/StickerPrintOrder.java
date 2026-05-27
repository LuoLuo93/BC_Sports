package com.bcsport.admin.entity.sticker;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sticker_print_order")
public class StickerPrintOrder {
    @TableId("id")
    private String id;
    @TableField("order_no")
    private String orderNo;
    @TableField("status")
    private Integer status;
    @TableField("applicant")
    private String applicant;
    @TableField("reviewer")
    private String reviewer;
    @TableField("review_time")
    private LocalDateTime reviewTime;
    @TableField("review_remark")
    private String reviewRemark;
    @TableField("remark")
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("create_by")
    private String createBy;
    @TableField("update_by")
    private String updateBy;
    @TableField("deleted")
    private Integer deleted;
    @TableField("print_time")
    private LocalDateTime printTime;
    @TableField("print_by")
    private String printBy;
    @TableField(exist = false)
    private List<StickerPrintOrderDetail> details;
}
