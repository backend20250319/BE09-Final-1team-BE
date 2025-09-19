package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum NewsCategory {
    POLITICS("정치", "🏛️"),
    ECONOMY("경제", "💰"),
    SOCIETY("사회", "👥"),
    LIFE("생활", "🎭"),
    INTERNATIONAL("세계", "🌍"),
    IT_SCIENCE("IT/과학", "💻"),
    VEHICLE("자동차/교통", "🚗"),
    TRAVEL_FOOD("여행/음식", "🧳"),
    ART("예술", "🎨");

    private final String categoryName;
    private final String icon;
    
    NewsCategory(String categoryName, String icon) {
        this.categoryName = categoryName;
        this.icon = icon;
    }
}
