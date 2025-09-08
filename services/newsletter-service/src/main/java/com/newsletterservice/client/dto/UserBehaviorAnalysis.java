package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 사용자 행동 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorAnalysis {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 평균 읽기 시간 (분)
     */
    private Double averageReadTimeMinutes;
    
    /**
     * 선호하는 읽기 시간대 (0-23)
     */
    private List<Integer> preferredReadingHours;
    
    /**
     * 선호하는 읽기 요일 (1-7, 월-일)
     */
    private List<Integer> preferredReadingDays;
    
    /**
     * 뉴스 소비 패턴 (예: "morning_reader", "evening_reader", "weekend_reader")
     */
    private String readingPattern;
    
    /**
     * 참여도 레벨 (LOW, MEDIUM, HIGH)
     */
    private String engagementLevel;
    
    /**
     * 클릭률 (0.0 ~ 1.0)
     */
    private Double clickThroughRate;
    
    /**
     * 평균 세션 시간 (분)
     */
    private Double averageSessionTimeMinutes;
    
    /**
     * 디바이스별 사용 패턴
     */
    private Map<String, Double> deviceUsagePattern;
    
    /**
     * 분석 기준일
     */
    private LocalDateTime analyzedAt;
    
    /**
     * 분석에 사용된 데이터 기간 (일)
     */
    private Integer analysisPeriodDays;
    
    /**
     * 카테고리별 선호도 점수 (0.0 ~ 1.0)
     */
    private Map<String, Double> categoryPreferences;
    
    /**
     * 카테고리별 읽기 횟수
     */
    private Map<String, Long> categoryReadCounts;
    
    /**
     * 총 읽기 횟수
     */
    private Long totalReadCount;
}
