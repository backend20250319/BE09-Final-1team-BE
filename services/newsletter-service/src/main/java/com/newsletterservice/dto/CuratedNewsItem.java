package com.newsletterservice.dto;

import com.newsletterservice.client.dto.NewsResponse;
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
public class CuratedNewsItem {
    private Long newsId;
    private String title;
    private String content;
    private String category;
    private double relevanceScore;
    private double engagementScore;
    private LocalDateTime publishedAt;
    private String source;
    private String imageUrl;
    private List<String> tags;
    private String summary;
    private int priority;
    private NewsResponse newsItem;
    private double totalScore;
    private double freshnessScore;
    private double qualityScore;
    private LocalDateTime curatedAt;
}
