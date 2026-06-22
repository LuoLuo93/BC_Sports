package com.bcsport.admin.dto;

import lombok.Data;

/**
 * 企微朋友圈查询传输对象
 */
@Data
public class QywxMomentQueryDTO {

    /**
     * 发布者姓名（模糊搜索）
     */
    private String creatorName;
}
