package com.bcsport.admin.dto.ydkl;

import lombok.Data;

import java.util.List;

@Data
public class YdCustomerFlowResponse {
    private List<Content> content;
    private Metadata metadata;

    @Data
    public static class Content {
        private String storeIdUuid;
        private String storeCode;
        private String name;
        private String realTime;
        private String indoorCount;
        private String outdoorCount;
        private String statDimensionDayTime;
        private String statDimensionHourTime;
        private String count;
        private String outsum;
        private String areaCode;
        private String areaName;
        private String storeAreaIdUuid;
    }

    @Data
    public static class Metadata {
        private Integer page;
        private Integer size;
        private Integer totalElements;
        private Integer totalPages;
    }
}
