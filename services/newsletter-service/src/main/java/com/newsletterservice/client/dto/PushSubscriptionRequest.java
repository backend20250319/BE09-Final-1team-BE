package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 웹 푸시 구독 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequest {
    
    /**
     * FCM 토큰
     */
    private String fcmToken;
    
    /**
     * 구독 엔드포인트 URL
     */
    private String endpoint;
    
    /**
     * 푸시 서버 공개 키
     */
    private String p256dh;
    
    /**
     * 인증 비밀키
     */
    private String auth;
    
    /**
     * 사용자 에이전트 정보
     */
    private String userAgent;
}
