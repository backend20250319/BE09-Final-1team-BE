package com.newsletterservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class NewsletterContent {
    private Long newsletterId;
    private Long userId;
    private Boolean personalized;
    private String title;
    private String subtitle;
    private String featuredImageUrl;
    private LocalDateTime generatedAt;
    private List<Section> sections;
    private Map<String, Object> personalizationInfo;
    
    public boolean isPersonalized() {
        return personalized != null && personalized;
    }
    
    /**
     * 뉴스레터 요약 정보 생성
     * 
     * @return 요약 문자열
     */
    public String getSummary() {
        if (sections == null || sections.isEmpty()) {
            return title != null ? title : "뉴스레터";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(title != null ? title : "뉴스레터");
        summary.append(" - ");
        
        int articleCount = 0;
        for (Section section : sections) {
            if (section.getArticles() != null) {
                articleCount += section.getArticles().size();
            }
        }
        
        summary.append(articleCount).append("개의 기사");
        return summary.toString();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
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
    @Getter
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
        private Boolean isPersonalized;
    }
}
