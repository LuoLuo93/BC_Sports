package com.bcsport.admin.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 分页查询参数
 */
@Data
public class PageQuery implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页码，默认第1页
     */
    private Integer pageNum = 1;
    
    /**
     * 每页记录数，默认10条)
     */
    private Integer pageSize = 10;
    
    /**
     * 排序字段
     */
    private String orderBy;
    
    /**
     * 排序方向（asc或desc)
     */
    private String orderDirection = "asc";
    
    /**
     * 转换为MyBatis-Plus的Page对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            page.addOrder("asc".equalsIgnoreCase(orderDirection) 
                ? com.baomidou.mybatisplus.core.metadata.OrderItem.asc(orderBy)
                : com.baomidou.mybatisplus.core.metadata.OrderItem.desc(orderBy));
        }
        return page;
    }
}
