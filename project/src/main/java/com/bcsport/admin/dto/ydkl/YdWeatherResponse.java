package com.bcsport.admin.dto.ydkl;

import lombok.Data;

import java.util.List;

@Data
public class YdWeatherResponse {
    private List<Content> content;
    private Metadata metadata;

    @Data
    public static class Content {
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
    }

    @Data
    public static class Metadata {
        private Integer page;
        private Integer size;
        private Integer totalElements;
        private Integer totalPages;
    }
}
