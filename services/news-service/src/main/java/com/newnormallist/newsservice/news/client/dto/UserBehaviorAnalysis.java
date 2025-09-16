package com.newnormallist.newsservice.news.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 사용자 행동 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorAnalysis {
    
    private Long userId;
    private Map<String, Long> categoryReadCounts;
    private Map<String, Double> categoryPreferences;
    private String topCategory;
    private Long totalReadCount;
    private Double engagementScore;
    private String analysisSummary;
}
