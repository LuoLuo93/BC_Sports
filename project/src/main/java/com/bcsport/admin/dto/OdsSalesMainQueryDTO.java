package com.bcsport.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 数仓销售查看(ODS_SALES_MAIN)查询条件
 */
@Data
public class OdsSalesMainQueryDTO {

    /** 单据号(模糊) */
    private String billNo;

    /** 提交时间-起(yyyy-MM-dd，含当天) */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date billTimeStart;

    /** 提交时间-止(yyyy-MM-dd，含当天全天) */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date billTimeEnd;
}
