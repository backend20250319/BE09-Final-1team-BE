package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedNewsletterInfo {
    private Long id;
    private String title;
    private String content;
    private String category;
    private LocalDateTime publishedAt;
    private String author;
    private String imageUrl;
    private List<String> tags;
    private int readCount;
    private double engagementRate;
    private boolean isBookmarked;
    private String summary;
    private String sourceUrl;
    private int estimatedReadTime;
    private double relevanceScore;
}
