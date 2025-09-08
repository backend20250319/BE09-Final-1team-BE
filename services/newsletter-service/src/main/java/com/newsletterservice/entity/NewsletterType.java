package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum NewsletterType {
    // 카테고리 기반
    POLITICS_DAILY("정치 데일리", "daily", NewsCategory.POLITICS),
    ECONOMY_DAILY("경제 데일리", "daily", NewsCategory.ECONOMY),
    TECH_WEEKLY("테크 위클리", "weekly", NewsCategory.IT_SCIENCE),
    
    // 시간대 기반
    MORNING_BRIEFING("모닝 브리핑", "daily", null),
    EVENING_REPORT("이브닝 리포트", "daily", null),
    
    // 특별 기획
    WEEKLY_HIGHLIGHTS("주간 하이라이트", "weekly", null),
    TRENDING_NEWS("트렌딩 뉴스", "realtime", null),
    
    // 개인화
    PERSONALIZED_DAILY("개인 맞춤 데일리", "daily", null),
    CUSTOM("사용자 정의", "custom", null);
    
    private final String description;
    private final String frequency;
    private final NewsCategory defaultCategory;
    
    NewsletterType(String description, String frequency, NewsCategory defaultCategory) {
        this.description = description;
        this.frequency = frequency;
        this.defaultCategory = defaultCategory;
    }
}
