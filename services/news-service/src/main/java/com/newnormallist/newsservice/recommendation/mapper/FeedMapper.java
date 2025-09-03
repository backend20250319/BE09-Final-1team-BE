package com.newnormallist.newsservice.recommendation.mapper;

import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import com.newnormallist.newsservice.recommendation.entity.NewsEntity;
import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;
import com.newnormallist.newsservice.recommendation.entity.DedupState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// News 엔티티를 FeedItemDto로 변환하는 매퍼
public class FeedMapper {
    public static FeedItemDto toDto(NewsEntity newsEntity) {
        // publishedAt을 LocalDateTime으로 변환
        LocalDateTime publishedAt = null;
        if (newsEntity.getPublishedAt() != null && !newsEntity.getPublishedAt().isEmpty()) {
            try {
                if (newsEntity.getPublishedAt().contains("T")) {
                    // ISO 8601 형식: 2025-08-20T09:35:11
                    publishedAt = LocalDateTime.parse(newsEntity.getPublishedAt(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                } else {
                    // 일반 형식: 2025-08-20 09:35:11
                    publishedAt = LocalDateTime.parse(newsEntity.getPublishedAt(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (Exception e) {
                // 파싱 실패 시 null로 설정
                publishedAt = null;
            }
        }

        return FeedItemDto.builder()
            .newsId(newsEntity.getNewsId())
            .title(newsEntity.getTitle())
            .content(null) // 본문은 제외
            .press(newsEntity.getPress())
            .link(newsEntity.getLink())
            .summary(null) // summary 필드는 현재 NewsEntity에 없음
            .trusted(newsEntity.getTrusted() ? 1 : 0) // Boolean -> Integer 변환
            .publishedAt(publishedAt)
            .createdAt(newsEntity.getCreatedAt())
            .reporterName(newsEntity.getReporter())
            .categoryName(newsEntity.getCategoryName())
            .categoryDescription(getCategoryDescription(newsEntity.getCategoryName()))
            .dedupState(newsEntity.getDedupState())
            .dedupStateDescription(getDedupStateDescription(newsEntity.getDedupState()))
            .imageUrl(newsEntity.getImageUrl())
            .oidAid(newsEntity.getOidAid())
            .status(null) // status 필드는 현재 NewsEntity에 없음
            .updatedAt(newsEntity.getUpdatedAt())
            .build();
    }
    
    private static String getCategoryDescription(RecommendationCategory category) {
        if (category == null) return null;
        return switch (category) {
            case POLITICS -> "정치";
            case ECONOMY -> "경제";
            case SOCIETY -> "사회";
            case LIFE -> "생활";
            case INTERNATIONAL -> "국제";
            case IT_SCIENCE -> "IT/과학";
            case VEHICLE -> "자동차";
            case TRAVEL_FOOD -> "여행/음식";
            case ART -> "예술";
        };
    }
    
    private static String getDedupStateDescription(DedupState dedupState) {
        if (dedupState == null) return null;
        return switch (dedupState) {
            case REPRESENTATIVE -> "대표";
            case RELATED -> "관련";
            case KEPT -> "유지";
        };
    }
}
