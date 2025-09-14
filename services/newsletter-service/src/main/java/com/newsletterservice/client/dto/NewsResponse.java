package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponse {
    private Long newsId;
    private String title;
    private String content;
    private String summary;
    private String imageUrl;
    private String link;
    private String categoryName;
    private String publishedAt;
    private String createdAt;
    private Integer viewCount;
    private Long shareCount;
    
    public String getCategory() {
        return categoryName;
    }
}
