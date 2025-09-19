package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum RecommendationType {
    CONTENT_BASED("콘텐츠 기반"),
    COLLABORATIVE("협업 필터링"),
    HYBRID("하이브리드"),
    TRENDING("트렌딩 기반"),
    CATEGORY_BASED("카테고리 기반");
    
    private final String description;
    
    RecommendationType(String description) {
        this.description = description;
    }
}
