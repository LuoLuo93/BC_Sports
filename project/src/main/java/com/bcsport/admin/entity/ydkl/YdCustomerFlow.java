package com.bcsport.admin.entity.ydkl;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("yd_customerflow")
public class YdCustomerFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String storeIdUuid;
    private String storeCode;
    private String name;
    private String realTime;
    private String indoorCount;
    private String outdoorCount;
    private String statDimensionDayTime;
    private String statDimensionHourTime;
    private String outsum;
    private String areaCode;
    private String areaName;
    private String storeAreaIdUuid;
    private Date insertDate;
}
