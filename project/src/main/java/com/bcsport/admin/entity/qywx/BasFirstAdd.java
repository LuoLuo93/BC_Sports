package com.bcsport.admin.entity.qywx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户首次添加记录（对应 BC_SPORTS_QYWX.dbo.Bas_FirstAdd）
 */
@Data
@TableName("Bas_FirstAdd")
public class BasFirstAdd implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 客户id */
    private String customerId;

    /** 首次添加时间 */
    private String firstAddTime;

    /** 首次添加人部门 */
    private String firstAdderDept;

    /** 首次添加人店铺 */
    private String firstAdderStore;

    /** 首次添加人 */
    private String firstAdder;
}
