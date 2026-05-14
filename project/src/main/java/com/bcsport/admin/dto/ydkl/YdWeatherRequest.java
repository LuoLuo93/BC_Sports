package com.bcsport.admin.dto.ydkl;

import lombok.Data;

import java.util.List;

@Data
public class YdWeatherRequest {
    private List<String> storeIds;
    private List<String> storeCodes;
    private Long startTime;
    private Long endTime;
}
