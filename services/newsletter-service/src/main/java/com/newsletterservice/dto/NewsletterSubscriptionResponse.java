package com.newsletterservice.dto;

import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스레터 구독 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterSubscriptionResponse {
    
    /**
     * 구독 ID
     */
    private Long subscriptionId;
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 이메일 주소
     */
    private String email;
    
    /**
     * 구독 빈도
     */
    private SubscriptionFrequency frequency;
    
    /**
     * 구독 상태
     */
    private SubscriptionStatus status;
    
    /**
     * 선호 카테고리 목록
     */
    private List<String> preferredCategories;
    
    /**
     * 관심 키워드 목록
     */
    private List<String> keywords;
    
    /**
     * 발송 시간
     */
    private Integer sendTime;
    
    /**
     * 개인화 여부
     */
    private Boolean isPersonalized;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;
}
