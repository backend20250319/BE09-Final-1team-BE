package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오 토큰 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoTokenInfo {
    
    private Long id;
    private Integer expiresInMillis;
    private Integer appId;
    private Integer expiresIn;
}
