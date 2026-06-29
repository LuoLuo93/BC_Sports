package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户首次添加-导入日志
 */
@Data
@TableName("Bas_FirstAdd_ImportLog")
public class BasFirstAddImportLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 总行数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 状态：SUCCESS / PARTIAL / FAILED */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    private LocalDateTime createTime;

    /** 操作人 */
    private String createBy;
}
