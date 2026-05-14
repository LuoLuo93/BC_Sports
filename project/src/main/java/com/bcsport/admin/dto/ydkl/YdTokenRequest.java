package com.bcsport.admin.dto.ydkl;

import lombok.Data;

@Data
public class YdTokenRequest {
    private String account;
    private String enterpriseCode;
    private String password;
}
