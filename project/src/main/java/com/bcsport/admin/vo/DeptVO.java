package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门视图对象
 */
@Data
public class DeptVO {
    
    private String id;

    private String parentId;

    private String deptName;

    private Integer sort;

    private String leader;

    private String phone;

    private String email;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 子部门列表
     */
    private List<DeptVO> children;
}
