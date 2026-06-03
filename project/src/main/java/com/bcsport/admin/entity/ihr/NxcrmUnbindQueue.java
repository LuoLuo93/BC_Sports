package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 南讯 CRM 会员解绑队列。
 *
 * <p>业务侧往此表插入待解绑记录（nasOuid + shopId），定时任务 {@code nxcrmUnbindTask}
 * 扫描 status=0 的记录调用南讯解绑接口，处理完成后回写状态。
 *
 * <p>状态机：0(待处理) → 1(已完成) / 2(失败)
 */
@Data
@TableName("NxcrmUnbindQueue")
public class NxcrmUnbindQueue {

    /** 待处理 */
    public static final int STATUS_PENDING  = 0;
    /** 已完成 */
    public static final int STATUS_DONE     = 1;
    /** 失败（已重试 MAX_RETRY 次仍失败） */
    public static final int STATUS_FAILED   = 2;

    /** 主键（IdWorker 雪花 ID，业务侧插入时生成） */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 外部门店 ID（如 BCHY） */
    @TableField("shopId")
    private String shopId;

    /** 南讯会员 ID */
    @TableField("nasOuid")
    private String nasOuid;

    /** 手机号（冗余便于排查，可为 null） */
    @TableField("mobile")
    private String mobile;

    /** 状态：0/1/2 */
    @TableField("status")
    private Integer status;

    /** 累计重试次数 */
    @TableField("retryCount")
    private Integer retryCount;

    /** 业务侧插入时间 */
    @TableField("createTime")
    private Date createTime;

    /** 任务处理时间 */
    @TableField("processTime")
    private Date processTime;

    /** 失败原因（status=2 时填） */
    @TableField("errorMsg")
    private String errorMsg;
}
