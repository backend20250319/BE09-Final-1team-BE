package com.newsletterservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterContent {
    private Long newsletterId;
    private Long userId;
    private Boolean personalized;
    private String title;
    private String featuredImageUrl;
    private LocalDateTime generatedAt;
    private List<Section> sections;
    
    public boolean isPersonalized() {
        return personalized != null && personalized;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private String heading;
        private String title;
        private String sectionType; // "PERSONALIZED", "TRENDING", "CATEGORY", "LATEST"
        private String description;
        private List<Article> articles;
        private String sectionImageUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Article {
        private Long id;
        private String title;
        private String summary;
        private String category; // String 타입으로 유지 (카테고리명)
        private String url;
        private LocalDateTime publishedAt;
        private String imageUrl;
        private Long viewCount;
        private Long shareCount;
        private Double personalizedScore;
        private Double trendScore;
    }
}
