package com.bcsport.admin.common;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(1)
    private Integer pageNum = 1;

    /**
     * 每页记录数，默认10条
     */
    @Min(1)
    @Max(500)
    private Integer pageSize = 10;

    /**
     * 排序字段（仅允许字母、数字、下划线）
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
        // 二次保护：即使校验被绕过，也强制上限
        int safePageSize = Math.min(pageSize != null ? pageSize : 10, 500);
        int safePageNum = Math.max(pageNum != null ? pageNum : 1, 1);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(safePageNum, safePageSize);
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            // 防 SQL 注入：仅允许字母、数字、下划线
            String safeOrderBy = orderBy.trim().replaceAll("[^a-zA-Z0-9_]", "");
            if (!safeOrderBy.isEmpty()) {
                String direction = "asc".equalsIgnoreCase(orderDirection) ? "asc" : "desc";
                page.addOrder("asc".equals(direction)
                    ? com.baomidou.mybatisplus.core.metadata.OrderItem.asc(safeOrderBy)
                    : com.baomidou.mybatisplus.core.metadata.OrderItem.desc(safeOrderBy));
            }
        }
        return page;
    }
}
