package com.newnormallist.newsservice.recommendation.dto;

import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 디버깅/모니터링용. 카테고리와 점수 페어.
@Getter 
@AllArgsConstructor
public class CategoryScoreDto {
    private final RecommendationCategory category;
    private final double score;
}