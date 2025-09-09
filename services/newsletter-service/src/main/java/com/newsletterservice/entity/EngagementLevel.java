package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum EngagementLevel {
    VERY_LOW("매우 낮음", 0, 20),
    LOW("낮음", 20, 40),
    MEDIUM("보통", 40, 60),
    HIGH("높음", 60, 80),
    VERY_HIGH("매우 높음", 80, 100);
    
    private final String description;
    private final int minScore;
    private final int maxScore;
    
    EngagementLevel(String description, int minScore, int maxScore) {
        this.description = description;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }
    
    public static EngagementLevel fromScore(double score) {
        for (EngagementLevel level : values()) {
            if (score >= level.minScore && score < level.maxScore) {
                return level;
            }
        }
        return VERY_HIGH; // 100점인 경우
    }
}
