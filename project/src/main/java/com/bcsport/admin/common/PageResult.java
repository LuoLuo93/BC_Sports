package com.bcsport.admin.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页码
     */
    private Long pageNum;
    
    /**
     * 每页记录数
     */
    private Long pageSize;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Long pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    public PageResult() {}
    
    public PageResult(IPage<T> page) {
        this.pageNum = page.getCurrent();
        this.pageSize = page.getSize();
        this.total = page.getTotal();
        this.pages = page.getPages();
        this.records = page.getRecords();
        this.hasPrevious = page.getCurrent() > 1;
        this.hasNext = page.getCurrent() < page.getPages();
    }
    
    /**
     * 从IPage创建PageResult
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page);
    }
}
