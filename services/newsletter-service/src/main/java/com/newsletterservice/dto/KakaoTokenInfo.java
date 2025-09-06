package com.newsletterservice.dto;

import lombok.Data;

@Data
public class KakaoTokenInfo {
    private Long id;
    private Integer expiresInMillis;
    private Integer appId;
    private Integer expiresIn;
}
