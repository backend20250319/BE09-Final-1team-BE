package com.newnormallist.newsservice.recommendation.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;
import com.newnormallist.newsservice.recommendation.entity.DedupState;

// 뉴스 피드에 노출될 뉴스 정보

@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class FeedItemDto {
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
    private RecommendationCategory categoryName;
    private String categoryDescription;
    private DedupState dedupState;
    private String dedupStateDescription;
    private String imageUrl;
    private String oidAid;
    private String status;
    private LocalDateTime updatedAt;
}