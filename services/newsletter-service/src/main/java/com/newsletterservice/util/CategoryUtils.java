package com.newsletterservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Slf4j
@Validated
public final class CategoryUtils {

    private static final Map<String, String> KOREAN_TO_ENGLISH = Map.of(
            "정치", "POLITICS",
            "경제", "ECONOMY",
            "사회", "SOCIETY",
            "생활", "LIFE",
            "세계", "INTERNATIONAL",
            "IT/과학", "IT_SCIENCE",
            "자동차/교통", "VEHICLE",
            "여행/음식", "TRAVEL_FOOD",
            "예술", "ART"
    );

    private static final Map<String, String> ENGLISH_TO_KOREAN = Map.of(
            "POLITICS", "정치",
            "ECONOMY", "경제",
            "SOCIETY", "사회",
            "LIFE", "생활",
            "INTERNATIONAL", "세계",
            "IT_SCIENCE", "IT/과학",
            "VEHICLE", "자동차/교통",
            "TRAVEL_FOOD", "여행/음식",
            "ART", "예술"
    );

    private CategoryUtils() {
        // 유틸리티 클래스
    }

    /**
     * 한국어 카테고리를 영어로 변환
     */
    public static String toEnglish(String koreanCategory) {
        if (koreanCategory == null || koreanCategory.trim().isEmpty()) {
            return "POLITICS";
        }

        String normalized = koreanCategory.trim();
        String english = KOREAN_TO_ENGLISH.get(normalized);
        
        if (english != null) {
            return english;
        }

        // 이미 영어인 경우 대문자로 변환하여 반환
        String upperCase = normalized.toUpperCase();
        if (ENGLISH_TO_KOREAN.containsKey(upperCase)) {
            return upperCase;
        }

        log.warn("알 수 없는 카테고리: {}. 기본값 POLITICS 사용", koreanCategory);
        return "POLITICS";
    }

    /**
     * 영어 카테고리를 한국어로 변환
     */
    public static String toKorean(String englishCategory) {
        if (englishCategory == null || englishCategory.trim().isEmpty()) {
            return "뉴스";
        }

        String normalized = englishCategory.trim().toUpperCase();
        String korean = ENGLISH_TO_KOREAN.get(normalized);
        
        if (korean != null) {
            return korean;
        }

        log.warn("알 수 없는 영어 카테고리: {}. 기본값 '뉴스' 사용", englishCategory);
        return "뉴스";
    }

    /**
     * 유효한 카테고리인지 확인
     */
    public static boolean isValidCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }

        String normalized = category.trim();
        return KOREAN_TO_ENGLISH.containsKey(normalized) || 
               ENGLISH_TO_KOREAN.containsKey(normalized.toUpperCase());
    }
}
