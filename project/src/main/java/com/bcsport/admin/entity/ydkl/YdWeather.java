package com.bcsport.admin.entity.ydkl;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("yd_weather")
public class YdWeather implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String storeIdUuid;
    private String storeCode;
    private String storename;
    private String realTime;
    private String province;
    private String city;
    private String weather;
    private String minTemp;
    private String maxTemp;
    private String wind;
    private Date insertDate;
}
