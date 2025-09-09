package com.newsletterservice.service;

import com.newsletterservice.entity.UserNewsletterSubscription;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.UserBehaviorAnalysis;
import com.newsletterservice.client.dto.UserInterestResponse;
import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 향상된 개인화 서비스
 * 뉴스레터 전용 개인화 프로필 생성 및 활용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedPersonalizationService {

    private final UserNewsletterSubscriptionRepository subscriptionRepository;
    private final UserServiceClient userServiceClient;
    private final NewsletterContentService contentService;

    /**
     * 뉴스레터 전용 개인화 프로필 생성
     */
    public NewsletterPersonalizationProfile createPersonalizationProfile(Long userId) {
        try {
            log.info("사용자 {} 뉴스레터 개인화 프로필 생성 시작", userId);
            
            // 1. 뉴스레터 구독 설정 조회
            List<UserNewsletterSubscription> subscriptions = 
                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            // 2. 사용자 행동 데이터 조회
            UserBehaviorAnalysis behavior = null;
            try {
                behavior = userServiceClient.getUserBehaviorAnalysis(userId).getData();
            } catch (Exception e) {
                log.warn("사용자 {} 행동 데이터 조회 실패", userId, e);
            }
            
            // 3. 사용자 관심사 조회
            UserInterestResponse interests = null;
            try {
                interests = userServiceClient.getUserInterests(userId).getData();
            } catch (Exception e) {
                log.warn("사용자 {} 관심사 데이터 조회 실패", userId, e);
            }
            
            // 4. 개인화 점수 계산 (뉴스레터 전용)
            double personalizationScore = calculateNewsletterPersonalizationScore(subscriptions, behavior, interests);
            
            // 5. 최적 발송 시간 계산
            String optimalSendTime = calculateOptimalSendTime(userId, behavior);
            
            // 6. 선호 콘텐츠 유형 분석
            List<String> preferredContentTypes = analyzePreferredContentTypes(subscriptions, behavior);
            
            // 7. 키워드 기반 관심사 분석
            Map<String, List<String>> categoryKeywords = extractCategoryKeywords(subscriptions);
            
            NewsletterPersonalizationProfile profile = NewsletterPersonalizationProfile.builder()
                .userId(userId)
                .subscriptions(subscriptions)
                .personalizationScore(personalizationScore)
                .optimalSendTime(optimalSendTime)
                .preferredContentTypes(preferredContentTypes)
                .categoryKeywords(categoryKeywords)
                .behaviorAnalysis(behavior)
                .interests(interests)
                .createdAt(LocalDateTime.now())
                .build();
            
            log.info("사용자 {} 뉴스레터 개인화 프로필 생성 완료 - 점수: {}", userId, personalizationScore);
            
            return profile;
            
        } catch (Exception e) {
            log.error("사용자 {} 뉴스레터 개인화 프로필 생성 실패", userId, e);
            return NewsletterPersonalizationProfile.empty(userId);
        }
    }
    
    /**
     * 뉴스레터 전용 개인화 점수 계산
     */
    private double calculateNewsletterPersonalizationScore(
            List<UserNewsletterSubscription> subscriptions, 
            UserBehaviorAnalysis behavior,
            UserInterestResponse interests) {
        
        double score = 0.0;
        
        // 1. 구독 다양성 점수 (25%)
        score += Math.min(0.25, subscriptions.size() * 0.05);
        
        // 2. 개인화 설정 점수 (20%)
        long personalizedCount = subscriptions.stream()
            .mapToLong(s -> s.getIsPersonalized() ? 1 : 0)
            .sum();
        score += Math.min(0.20, personalizedCount * 0.05);
        
        // 3. 키워드 설정 점수 (20%)
        long keywordCount = subscriptions.stream()
            .mapToLong(s -> s.getKeywords() != null && !s.getKeywords().isEmpty() ? 1 : 0)
            .sum();
        score += Math.min(0.20, keywordCount * 0.05);
        
        // 4. 행동 기반 점수 (20%)
        if (behavior != null && behavior.getCategoryReadCounts() != null) {
            long totalReads = behavior.getCategoryReadCounts().values().stream()
                .mapToLong(Long::longValue).sum();
            score += Math.min(0.20, Math.log10(totalReads + 1) / Math.log10(101));
        }
        
        // 5. 관심사 일치 점수 (15%)
        if (interests != null && interests.getTopInterests() != null) {
            long matchingCategories = subscriptions.stream()
                .mapToLong(s -> interests.getTopInterests().contains(s.getCategory()) ? 1 : 0)
                .sum();
            score += Math.min(0.15, matchingCategories * 0.03);
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * 최적 발송 시간 계산
     */
    private String calculateOptimalSendTime(Long userId, UserBehaviorAnalysis behavior) {
        try {
            // UserServiceClient를 통해 사용자의 최적 발송 시간 조회
            // String optimalTime = userServiceClient.getOptimalNewsletterSendTime(userId); // UserServiceClient에 구현 필요
            
            // 기본값: 오전 8시
            return "08:00";
            
        } catch (Exception e) {
            log.error("사용자 {} 최적 발송 시간 계산 실패", userId, e);
            return "08:00";
        }
    }
    
    /**
     * 선호 콘텐츠 유형 분석
     */
    private List<String> analyzePreferredContentTypes(
            List<UserNewsletterSubscription> subscriptions, 
            UserBehaviorAnalysis behavior) {
        
        List<String> contentTypes = new ArrayList<>();
        
        // 1. 구독 카테고리 기반 분석
        Set<String> subscribedCategories = subscriptions.stream()
            .map(UserNewsletterSubscription::getCategory)
            .collect(Collectors.toSet());
        
        contentTypes.addAll(subscribedCategories);
        
        // 2. 행동 데이터 기반 분석
        if (behavior != null && behavior.getCategoryPreferences() != null) {
            List<String> topCategories = behavior.getCategoryPreferences().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            contentTypes.addAll(topCategories);
        }
        
        return contentTypes.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 키워드 추출
     */
    private Map<String, List<String>> extractCategoryKeywords(List<UserNewsletterSubscription> subscriptions) {
        Map<String, List<String>> categoryKeywords = new HashMap<>();
        
        for (UserNewsletterSubscription subscription : subscriptions) {
            if (subscription.getKeywords() != null && !subscription.getKeywords().isEmpty()) {
                try {
                    // JSON 파싱 (실제 구현에서는 ObjectMapper 사용)
                    List<String> keywords = parseKeywords(subscription.getKeywords());
                    categoryKeywords.put(subscription.getCategory(), keywords);
                } catch (Exception e) {
                    log.error("키워드 파싱 실패: {}", subscription.getKeywords(), e);
                }
            }
        }
        
        return categoryKeywords;
    }
    
    /**
     * 키워드 JSON 파싱 (간단한 구현)
     */
    private List<String> parseKeywords(String keywordsJson) {
        // 실제 구현에서는 ObjectMapper를 사용하여 JSON 파싱
        // 여기서는 간단한 문자열 분리로 구현
        if (keywordsJson.startsWith("[") && keywordsJson.endsWith("]")) {
            String content = keywordsJson.substring(1, keywordsJson.length() - 1);
            return Arrays.stream(content.split(","))
                .map(s -> s.trim().replace("\"", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        
        return Arrays.stream(keywordsJson.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * 개인화된 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent generatePersonalizedContent(Long userId, List<UserNewsletterSubscription> subscriptions) {
        try {
            log.info("사용자 {} 개인화 뉴스레터 콘텐츠 생성 시작", userId);
            
            // 1. 개인화 프로필 생성
            NewsletterPersonalizationProfile profile = createPersonalizationProfile(userId);
            
            // 2. 구독 카테고리별 콘텐츠 생성
            Map<String, Object> personalizedData = new HashMap<>();
            personalizedData.put("profile", profile);
            personalizedData.put("subscriptions", subscriptions);
            personalizedData.put("personalizationScore", profile.getPersonalizationScore());
            personalizedData.put("preferredContentTypes", profile.getPreferredContentTypes());
            personalizedData.put("categoryKeywords", profile.getCategoryKeywords());
            
            // 3. 뉴스레터 콘텐츠 생성
            // NewsletterContent content = contentService.generatePersonalizedContent(userId, personalizedData); // NewsletterContentService에 구현 필요
            
            // 임시 구현: 기본 콘텐츠 생성
            NewsletterContent content = new NewsletterContent();
            content.setUserId(userId);
            content.setTitle("개인화된 뉴스레터");
            // content.setContent("개인화된 뉴스레터 콘텐츠입니다."); // NewsletterContent에 setContent 메서드 구현 필요
            
            log.info("사용자 {} 개인화 뉴스레터 콘텐츠 생성 완료", userId);
            
            return content;
            
        } catch (Exception e) {
            log.error("사용자 {} 개인화 뉴스레터 콘텐츠 생성 실패", userId, e);
            // 폴백: 기본 콘텐츠 생성
            NewsletterContent fallbackContent = new NewsletterContent();
            fallbackContent.setUserId(userId);
            fallbackContent.setTitle("기본 뉴스레터");
            // fallbackContent.setContent("기본 뉴스레터 콘텐츠입니다."); // NewsletterContent에 setContent 메서드 구현 필요
            return fallbackContent;
        }
    }
    
    /**
     * 개인화 설정 업데이트
     */
    public void updatePersonalizationSettings(Long userId, PersonalizationSettingsRequest request) {
        try {
            log.info("사용자 {} 개인화 설정 업데이트 시작", userId);
            
            // 1. 기존 구독 설정 업데이트
            for (PersonalizationSettingsRequest.CategorySetting setting : request.getCategorySettings()) {
                List<UserNewsletterSubscription> subscriptions = 
                    subscriptionRepository.findAllByUserIdAndCategory(userId, setting.getCategory());
                
                for (UserNewsletterSubscription subscription : subscriptions) {
                    if (setting.getFrequency() != null) {
                        subscription.setFrequency(setting.getFrequency());
                    }
                    if (setting.getSendTime() != null) {
                        subscription.setSendTime(setting.getSendTime());
                    }
                    if (setting.getIsPersonalized() != null) {
                        subscription.setIsPersonalized(setting.getIsPersonalized());
                    }
                    if (setting.getKeywords() != null) {
                        subscription.setKeywords(setting.getKeywords());
                    }
                    
                    subscriptionRepository.save(subscription);
                }
            }
            
            // 2. 개인화 프로필 재계산
            NewsletterPersonalizationProfile updatedProfile = createPersonalizationProfile(userId);
            
            log.info("사용자 {} 개인화 설정 업데이트 완료 - 점수: {}", userId, updatedProfile.getPersonalizationScore());
            
        } catch (Exception e) {
            log.error("사용자 {} 개인화 설정 업데이트 실패", userId, e);
            throw new RuntimeException("개인화 설정 업데이트 실패", e);
        }
    }
    
    /**
     * 뉴스레터 개인화 프로필 DTO
     */
    public static class NewsletterPersonalizationProfile {
        private final Long userId;
        private final List<UserNewsletterSubscription> subscriptions;
        private final double personalizationScore;
        private final String optimalSendTime;
        private final List<String> preferredContentTypes;
        private final Map<String, List<String>> categoryKeywords;
        private final UserBehaviorAnalysis behaviorAnalysis;
        private final UserInterestResponse interests;
        private final LocalDateTime createdAt;
        
        private NewsletterPersonalizationProfile(Builder builder) {
            this.userId = builder.userId;
            this.subscriptions = builder.subscriptions;
            this.personalizationScore = builder.personalizationScore;
            this.optimalSendTime = builder.optimalSendTime;
            this.preferredContentTypes = builder.preferredContentTypes;
            this.categoryKeywords = builder.categoryKeywords;
            this.behaviorAnalysis = builder.behaviorAnalysis;
            this.interests = builder.interests;
            this.createdAt = builder.createdAt;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static NewsletterPersonalizationProfile empty(Long userId) {
            return builder()
                .userId(userId)
                .subscriptions(Collections.emptyList())
                .personalizationScore(0.0)
                .optimalSendTime("08:00")
                .preferredContentTypes(Collections.emptyList())
                .categoryKeywords(Collections.emptyMap())
                .createdAt(LocalDateTime.now())
                .build();
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public List<UserNewsletterSubscription> getSubscriptions() { return subscriptions; }
        public double getPersonalizationScore() { return personalizationScore; }
        public String getOptimalSendTime() { return optimalSendTime; }
        public List<String> getPreferredContentTypes() { return preferredContentTypes; }
        public Map<String, List<String>> getCategoryKeywords() { return categoryKeywords; }
        public UserBehaviorAnalysis getBehaviorAnalysis() { return behaviorAnalysis; }
        public UserInterestResponse getInterests() { return interests; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        public static class Builder {
            private Long userId;
            private List<UserNewsletterSubscription> subscriptions;
            private double personalizationScore;
            private String optimalSendTime;
            private List<String> preferredContentTypes;
            private Map<String, List<String>> categoryKeywords;
            private UserBehaviorAnalysis behaviorAnalysis;
            private UserInterestResponse interests;
            private LocalDateTime createdAt;
            
            public Builder userId(Long userId) { this.userId = userId; return this; }
            public Builder subscriptions(List<UserNewsletterSubscription> subscriptions) { this.subscriptions = subscriptions; return this; }
            public Builder personalizationScore(double personalizationScore) { this.personalizationScore = personalizationScore; return this; }
            public Builder optimalSendTime(String optimalSendTime) { this.optimalSendTime = optimalSendTime; return this; }
            public Builder preferredContentTypes(List<String> preferredContentTypes) { this.preferredContentTypes = preferredContentTypes; return this; }
            public Builder categoryKeywords(Map<String, List<String>> categoryKeywords) { this.categoryKeywords = categoryKeywords; return this; }
            public Builder behaviorAnalysis(UserBehaviorAnalysis behaviorAnalysis) { this.behaviorAnalysis = behaviorAnalysis; return this; }
            public Builder interests(UserInterestResponse interests) { this.interests = interests; return this; }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            
            public NewsletterPersonalizationProfile build() {
                return new NewsletterPersonalizationProfile(this);
            }
        }
    }
    
    /**
     * 개인화 설정 요청 DTO
     */
    public static class PersonalizationSettingsRequest {
        private List<CategorySetting> categorySettings;
        
        public List<CategorySetting> getCategorySettings() { return categorySettings; }
        public void setCategorySettings(List<CategorySetting> categorySettings) { this.categorySettings = categorySettings; }
        
        public static class CategorySetting {
            private String category;
            private String frequency;
            private String sendTime;
            private Boolean isPersonalized;
            private String keywords;
            
            // Getters and Setters
            public String getCategory() { return category; }
            public void setCategory(String category) { this.category = category; }
            public String getFrequency() { return frequency; }
            public void setFrequency(String frequency) { this.frequency = frequency; }
            public String getSendTime() { return sendTime; }
            public void setSendTime(String sendTime) { this.sendTime = sendTime; }
            public Boolean getIsPersonalized() { return isPersonalized; }
            public void setIsPersonalized(Boolean isPersonalized) { this.isPersonalized = isPersonalized; }
            public String getKeywords() { return keywords; }
            public void setKeywords(String keywords) { this.keywords = keywords; }
        }
    }
}
