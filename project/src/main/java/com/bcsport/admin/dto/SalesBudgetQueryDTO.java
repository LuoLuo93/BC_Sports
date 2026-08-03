package com.bcsport.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 店铺日预算查询条件
 */
@Data
public class SalesBudgetQueryDTO {

    /** 店铺名称(模糊) */
    private String storeName;

    /** 品牌名称(模糊) */
    private String brandName;

    /** 预算日期-起(yyyy-MM-dd) */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date budgetDtmStart;

    /** 预算日期-止(yyyy-MM-dd) */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date budgetDtmEnd;

    /** 一级组织(模糊) */
    private String department1;

    /** 业务类型(模糊) */
    private String professionType;
}
