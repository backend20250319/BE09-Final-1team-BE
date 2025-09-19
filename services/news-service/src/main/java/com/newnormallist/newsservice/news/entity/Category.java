package com.newnormallist.newsservice.news.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
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
}
