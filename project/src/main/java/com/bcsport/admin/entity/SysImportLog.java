package com.bcsport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全系统统一导入日志（R01），主库 BC_SPORTS_SYS_IMPORT_LOG。
 * 各模块日志页按 importType 过滤本表；旧各模块 *_IMPORT_LOG 表切换后停写。
 */
@Data
@TableName("BC_SPORTS_SYS_IMPORT_LOG")
public class SysImportLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主库 GlobalConfig 默认 ASSIGN_ID，这里必须显式 AUTO 交给 Oracle IDENTITY */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模块标识，见 ImportType */
    private String importType;

    private String fileName;
    private Long fileSize;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;

    /** SUCCESS / PARTIAL / FAILED */
    private String status;

    private String errorMsg;
    private LocalDateTime createTime;
    private String createBy;

    /** 历史搬迁来源行 ID，新写入为 NULL */
    private Long legacyId;
}
