package com.newsletterservice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 뉴스레터 서비스 레벨 정의
 * 사용자의 인증 상태와 구독 여부에 따라 서비스 역량을 차별화
 */
@Getter
@RequiredArgsConstructor
public enum ServiceLevel {
    
    /**
     * 📰 공개 서비스 - 비로그인 사용자
     * 기본 카테고리 9개, 각 카테고리당 5개 뉴스, 일반 트렌딩 키워드
     */
    PUBLIC("PUBLIC", "공개 서비스", false, false, 5, 9, 
           new String[]{"기본 뉴스", "트렌딩 키워드", "인기 카테고리"},
           new String[]{"제한된 뉴스 수", "개인화 없음", "구독 관리 불가"},
           "🔐 로그인하면 맞춤 뉴스를 받아보실 수 있어요!"),
    
    /**
     * 🔐 인증 기본 서비스 - 로그인했지만 구독 없음
     * 확장된 카테고리, 각 카테고리당 7개 뉴스, 구독 유도
     */
    AUTHENTICATED_BASIC("AUTHENTICATED_BASIC", "인증 기본 서비스", true, false, 7, 9,
                       new String[]{"확장된 뉴스", "구독 관리", "읽기 기록"},
                       new String[]{"개인화 제한적", "AI 추천 없음"},
                       "🎯 관심 카테고리를 구독하면 맞춤 뉴스를 받아보실 수 있어요!"),
    
    /**
     * 🎯 개인화 프리미엄 서비스 - 구독 설정 완료
     * 구독 카테고리 기반 뉴스 (2배 더 많이), 개인화된 트렌딩 키워드, AI 추천
     */
    PERSONALIZED_PREMIUM("PERSONALIZED_PREMIUM", "개인화 프리미엄 서비스", true, true, 10, 9,
                        new String[]{"완전 개인화", "AI 추천", "맞춤 키워드", "최적 발송시간"},
                        new String[]{},
                        "✨ 완전한 개인화 서비스를 이용하고 계세요!");

    private final String code;
    private final String description;
    private final boolean userAuthenticated;
    private final boolean personalizationEnabled;
    private final int maxNewsPerCategory;
    private final int maxTrendingKeywords;
    private final String[] features;
    private final String[] limitations;
    private final String upgradeMessage;

    /**
     * 사용자 상태에 따른 서비스 레벨 결정
     */
    public static ServiceLevel determineLevel(Boolean isAuthenticated, Integer subscriptionCount) {
        if (isAuthenticated == null || !isAuthenticated) {
            return PUBLIC;
        }
        
        if (subscriptionCount == null || subscriptionCount == 0) {
            return AUTHENTICATED_BASIC;
        }
        
        return PERSONALIZED_PREMIUM;
    }

    /**
     * 서비스 역량 정보를 Map으로 반환
     */
    public java.util.Map<String, Object> getCapabilities() {
        java.util.Map<String, Object> capabilities = new java.util.HashMap<>();
        capabilities.put("level", this.code);
        capabilities.put("description", this.description);
        capabilities.put("userAuthenticated", this.userAuthenticated);
        capabilities.put("personalizationEnabled", this.personalizationEnabled);
        capabilities.put("maxNewsPerCategory", this.maxNewsPerCategory);
        capabilities.put("maxTrendingKeywords", this.maxTrendingKeywords);
        capabilities.put("features", this.features);
        capabilities.put("limitations", this.limitations);
        capabilities.put("upgradeMessage", this.upgradeMessage);
        return capabilities;
    }

    /**
     * 다음 레벨로 업그레이드 가능한지 확인
     */
    public boolean canUpgrade() {
        return this != PERSONALIZED_PREMIUM;
    }

    /**
     * 다음 레벨 반환
     */
    public ServiceLevel getNextLevel() {
        switch (this) {
            case PUBLIC:
                return AUTHENTICATED_BASIC;
            case AUTHENTICATED_BASIC:
                return PERSONALIZED_PREMIUM;
            default:
                return PERSONALIZED_PREMIUM;
        }
    }
}
