package com.newsletterservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 웹 푸시 구독 정보를 나타내는 모델 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscription {
    
    /**
     * 구독자 ID
     */
    private String subscriptionId;
    
    /**
     * 사용자 ID
     */
    private String userId;
    
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
     * 구독 상태 (ACTIVE, INACTIVE, EXPIRED)
     */
    private String status;
    
    /**
     * 구독 생성 시간
     */
    private Long createdAt;
    
    /**
     * 구독 만료 시간
     */
    private Long expiresAt;
    
    /**
     * 사용자 에이전트 정보
     */
    private String userAgent;
    
    /**
     * 구독이 활성 상태인지 확인
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) && 
               (expiresAt == null || expiresAt > System.currentTimeMillis());
    }
}
