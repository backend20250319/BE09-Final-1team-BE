package com.newnormallist.newsservice.news.dto;

import com.newnormallist.newsservice.news.entity.NewsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {
    private Long newsId;
    private String title;
    private String content;
    private String press;
    private String link;
    private String summary;
    private Integer trusted;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String reporterName;
    private String categoryName;
    private String categoryDescription;
    private String dedupState;
    private String dedupStateDescription;
    private String imageUrl;
    private String oidAid;
    private NewsStatus status;
    private LocalDateTime updatedAt;
    private Long viewCount;
}