package com.newsletterservice.service;

import com.newsletterservice.entity.UserNewsletterSubscription;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 뉴스레터 서비스 레벨 관리 컴포넌트
 * 사용자의 인증 상태와 구독 여부에 따라 서비스 역량을 차별화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsletterServiceLevel {

    private final UserNewsletterSubscriptionRepository subscriptionRepository;

    /**
     * 사용자의 서비스 레벨 결정
     */
    public ServiceCapability determineServiceLevel(Long userId) {
        if (userId == null) {
            log.debug("비로그인 사용자 - PUBLIC 레벨");
            return ServiceCapability.PUBLIC;
        }

        // 활성 구독 조회
        List<UserNewsletterSubscription> activeSubscriptions = 
            subscriptionRepository.findActiveSubscriptionsByUserId(userId);

        if (activeSubscriptions.isEmpty()) {
            log.debug("로그인 사용자(구독 없음) - AUTHENTICATED_BASIC 레벨: userId={}", userId);
            return ServiceCapability.AUTHENTICATED_BASIC;
        } else {
            log.debug("구독 사용자 - PERSONALIZED_PREMIUM 레벨: userId={}, subscriptions={}", 
                    userId, activeSubscriptions.size());
            return ServiceCapability.PERSONALIZED_PREMIUM;
        }
    }

    /**
     * 서비스 레벨별 뉴스 제한 수 반환
     */
    public int getNewsLimitPerCategory(ServiceCapability capability) {
        return switch (capability) {
            case PUBLIC -> 5;                    // 📰 공개: 카테고리당 5개
            case AUTHENTICATED_BASIC -> 7;       // 🔐 로그인: 카테고리당 7개
            case PERSONALIZED_PREMIUM -> 10;     // 🎯 구독자: 카테고리당 10개 (2배)
        };
    }

    /**
     * 서비스 레벨별 전체 카테고리 목록
     */
    public String[] getAvailableCategories(ServiceCapability capability) {
        // 모든 레벨에서 동일한 9개 카테고리 제공
        return new String[]{
            "정치", "경제", "사회", "생활", "세계", 
            "IT/과학", "자동차/교통", "여행/음식", "예술"
        };
    }

    /**
     * 서비스 레벨별 트렌딩 키워드 수
     */
    public int getTrendingKeywordsLimit(ServiceCapability capability) {
        return switch (capability) {
            case PUBLIC -> 8;                    // 📰 공개: 8개
            case AUTHENTICATED_BASIC -> 10;      // 🔐 로그인: 10개
            case PERSONALIZED_PREMIUM -> 12;     // 🎯 구독자: 12개
        };
    }

    /**
     * 서비스 레벨별 기능 제공 여부
     */
    public Map<String, Boolean> getFeatureAvailability(ServiceCapability capability) {
        return switch (capability) {
            case PUBLIC -> Map.of(
                "subscriptionManagement", false,
                "aiRecommendations", false,
                "personalizedKeywords", false,
                "userStats", false,
                "upgradePrompt", true
            );
            
            case AUTHENTICATED_BASIC -> Map.of(
                "subscriptionManagement", true,
                "aiRecommendations", false,
                "personalizedKeywords", false,
                "userStats", true,
                "subscriptionPrompt", true
            );
            
            case PERSONALIZED_PREMIUM -> Map.of(
                "subscriptionManagement", true,
                "aiRecommendations", true,
                "personalizedKeywords", true,
                "userStats", true,
                "fullPersonalization", true
            );
        };
    }

    /**
     * 서비스 역량 정의
     */
    public enum ServiceCapability {
        PUBLIC("공개 서비스", "📰 기본 뉴스 제공"),
        AUTHENTICATED_BASIC("기본 서비스", "🔐 로그인 사용자 확장 서비스"),
        PERSONALIZED_PREMIUM("프리미엄 서비스", "🎯 완전 개인화 서비스");

        private final String displayName;
        private final String description;

        ServiceCapability(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
