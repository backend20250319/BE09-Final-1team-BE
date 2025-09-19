package com.newnormallist.crawlerservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Python 중복제거 서비스 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeduplicationResponse {
    
    /**
     * 처리된 카테고리
     */
    private String category;
    
    /**
     * 원본 뉴스 개수
     */
    private int originalCount;
    
    /**
     * 중복제거 후 뉴스 개수
     */
    private int deduplicatedCount;
    
    /**
     * 연관뉴스 개수
     */
    private int relatedCount;
    
    /**
     * 제거된 뉴스 개수
     */
    private int removedCount;
    
    /**
     * 처리 시간 (초)
     */
    private double processingTimeSeconds;
    
    /**
     * 상세 통계
     */
    private Map<String, Object> statistics;
    
    /**
     * 처리 결과 메시지
     */
    private String message;
    
    /**
     * 제거율 계산
     */
    public double getRemovalRate() {
        return originalCount > 0 ? (double) removedCount / originalCount : 0.0;
    }
    
    /**
     * 처리 성공 여부
     */
    public boolean isSuccessful() {
        return deduplicatedCount >= 0 && processingTimeSeconds >= 0;
    }
}
