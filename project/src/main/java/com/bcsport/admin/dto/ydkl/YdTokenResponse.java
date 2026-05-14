package com.bcsport.admin.dto.ydkl;

import lombok.Data;

@Data
public class YdTokenResponse {
    private String accessToken;
    private String tokenType;
    private String expiresIn;
    private String account;
    private String userId;
    private String refreshToken;
    private String refreshTokenExpiresIn;
    private String scope;
    private String nickname;
    private String originalId;
}
