package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工自定义属性 */
@Data
@TableName("employee_flex_attributes")
public class IhrEmployeeFlexAttr implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    private String dCodeType1;
    private String dCodeType2;
    private String dCodeType3;
    private String dCodeType4;
    private String dBoolean0;
    private String dCodeType5;
    private String dCodeType6;
    private String dDate2;
    private String dCodeType7;
    private String dCodeType8;
    private String dCodeType9;
    private Date createTime;
    private Date updateTime;
}
