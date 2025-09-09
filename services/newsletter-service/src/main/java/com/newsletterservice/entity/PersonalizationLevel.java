package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum PersonalizationLevel {
    NONE("개인화 없음", 0),
    BASIC("기본", 1),
    INTERMEDIATE("중급", 2),
    ADVANCED("고급", 3),
    PREMIUM("프리미엄", 4);
    
    private final String description;
    private final int level;
    
    PersonalizationLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }
}
