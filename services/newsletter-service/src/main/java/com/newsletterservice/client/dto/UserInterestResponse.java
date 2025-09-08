package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 사용자 관심사 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestResponse {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 카테고리별 관심도 점수 (0.0 ~ 1.0)
     */
    private Map<String, Double> categoryScores;
    
    /**
     * 상위 관심 카테고리 목록
     */
    private List<String> topCategories;
    
    /**
     * 상위 관심사 목록 (topCategories와 동일한 데이터)
     */
    private List<String> topInterests;
    
    /**
     * 관심사 분석 신뢰도 (0.0 ~ 1.0)
     */
    private Double confidence;
    
    /**
     * 분석 기준일
     */
    private String analyzedAt;
    
    /**
     * 분석에 사용된 데이터 기간 (일)
     */
    private Integer analysisPeriodDays;
}
