package com.bcsport.admin.dto.ydkl;

import lombok.Data;

import java.util.List;

@Data
public class YdCustomerFlowRequest {
    private List<String> storeIds;
    private Long startTime;
    private Long endTime;
    private String timeType;
    private List<String> groupIds;
    private List<String> areaIds;
    private List<String> storeCodes;
}
