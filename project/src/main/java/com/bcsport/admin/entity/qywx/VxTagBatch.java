package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企微批量打标批次汇总
 */
@Data
@TableName("VX_TagBatch")
public class VxTagBatch {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("batchNo")
    private String batchNo;

    @TableField("fileName")
    private String fileName;

    /** RUNNING / DONE / FAILED */
    @TableField("status")
    private String status;

    @TableField("totalRows")
    private Integer totalRows;

    @TableField("successCnt")
    private Integer successCnt;

    @TableField("failCnt")
    private Integer failCnt;

    @TableField("unmatchedTagRows")
    private Integer unmatchedTagRows;

    @TableField("unmatchedCustomerRows")
    private Integer unmatchedCustomerRows;

    @TableField("errmsg")
    private String errmsg;

    @TableField("startTime")
    private LocalDateTime startTime;

    @TableField("endTime")
    private LocalDateTime endTime;

    @TableField("createTime")
    private LocalDateTime createTime;
}
