package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum SubscriptionFrequency {
    DAILY("매일"),
    WEEKLY("주간"),
    MONTHLY("월간"),
    IMMEDIATE("즉시"); // 실시간 알림용

    private final String description;
    
    SubscriptionFrequency(String description) {
        this.description = description;
    }
}