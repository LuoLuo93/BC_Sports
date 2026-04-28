package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工自定义子集 (tab_staff_subset04)
 */
@Data
@TableName("employee_tab_staff_subset04")
public class IhrEmployeeSubset04 implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("staffId")
    private String staffId;

    private String dCodeType0;
    private String dCodeType1;
    private String dCodeType2;
    private String dString1;
    private String dCodeType3;
    private String dCodeType4;
    private String dCodeType5;
    private String dString0;
    private String dCodeType6;
    private Date createTime;
    private Date updateTime;
}
