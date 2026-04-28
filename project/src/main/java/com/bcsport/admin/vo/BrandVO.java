package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 品牌视图对象
 */
@Data
public class BrandVO {
    
    private String id;
    
    private String brandName;
    
    private String brandLogo;
    
    private String description;
    
    private Integer status;
    
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
