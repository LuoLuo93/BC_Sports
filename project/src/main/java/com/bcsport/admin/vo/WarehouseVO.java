package com.bcsport.admin.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 仓库视图对象
 */
@Data
public class WarehouseVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 仓库ID
     */
    private String id;
    
    /**
     * 仓库名称
     */
    private String warehouseName;
    
    /**
     * 仓库编码
     */
    private String warehouseCode;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 创建时间
     */
    private String createBy;
    
    /**
     * 更新成
     */
    private String updateBy;
}
