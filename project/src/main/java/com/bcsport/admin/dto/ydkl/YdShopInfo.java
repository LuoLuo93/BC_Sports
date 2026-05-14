package com.bcsport.admin.dto.ydkl;

import lombok.Data;

import java.util.List;

@Data
public class YdShopInfo {
    private String storeId;
    private String storeName;
    private String storeCode;
    private String address;
    private String businessHoursEnd;
    private String businessHoursStart;
    private String groupId;
    private List<String> groupIds;
    private String groupName;
    private Double latitude;
    private Double longitude;
    private String status;
    private String type;
    private String storeTag;
    private String area;
    private String city;
    private String province;
}
