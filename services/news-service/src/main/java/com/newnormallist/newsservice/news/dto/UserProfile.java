package com.newnormallist.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 사용자 프로필 정보를 담는 DTO
 * 뉴스 추천 시스템에서 사용자 특성을 파악하기 위해 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    private Long userId;
    private boolean isNewUser;
    private List<String> preferredCategories;
    private Map<String, Long> categoryReadCounts;
    private List<String> subscribedKeywords;
    private int totalReadCount;
    private double personalizationScore;
    
    /**
     * 신규 사용자 여부를 판단하는 메서드
     * 읽기 기록이 적거나 선호 카테고리가 없는 경우 신규 사용자로 간주
     */
    public boolean isNewUser() {
        return totalReadCount < 10 || 
               (preferredCategories == null || preferredCategories.isEmpty()) ||
               (categoryReadCounts == null || categoryReadCounts.isEmpty());
    }
    
    /**
     * 개인화 점수를 기반으로 한 사용자 분류
     */
    public UserType getUserType() {
        if (isNewUser()) {
            return UserType.NEW_USER;
        } else if (personalizationScore > 0.7) {
            return UserType.HIGHLY_PERSONALIZED;
        } else if (personalizationScore > 0.3) {
            return UserType.MODERATELY_PERSONALIZED;
        } else {
            return UserType.LOW_PERSONALIZATION;
        }
    }
    
    /**
     * 사용자 타입 열거형
     */
    public enum UserType {
        NEW_USER,           // 신규 사용자
        LOW_PERSONALIZATION, // 낮은 개인화 수준
        MODERATELY_PERSONALIZED, // 중간 개인화 수준
        HIGHLY_PERSONALIZED // 높은 개인화 수준
    }
}
