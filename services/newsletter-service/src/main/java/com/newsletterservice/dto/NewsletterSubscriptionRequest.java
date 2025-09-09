package com.newsletterservice.dto;

import com.newsletterservice.entity.SubscriptionFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 뉴스레터 구독 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterSubscriptionRequest {
    
    /**
     * 사용자 이메일 주소
     */
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
    
    /**
     * 구독 빈도
     */
    @NotNull(message = "구독 빈도는 필수입니다")
    private SubscriptionFrequency frequency;
    
    /**
     * 선호 카테고리 목록
     */
    private List<String> preferredCategories;
    
    /**
     * 관심 키워드 목록
     */
    private List<String> keywords;
    
    /**
     * 발송 시간 (0-23)
     */
    private Integer sendTime;
    
    /**
     * 개인화 여부
     */
    @Builder.Default
    private Boolean isPersonalized = true;
    
    /**
     * 인증 여부 (프론트엔드에서 전달)
     */
    private Boolean hasAuth;
}
