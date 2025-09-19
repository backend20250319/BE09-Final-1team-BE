package com.newnormallist.crawlerservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 연관뉴스 상세 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedNewsDetail {
    
    /**
     * 대표 뉴스 OID_AID
     */
    private String repOidAid;
    
    /**
     * 연관 뉴스 OID_AID
     */
    private String relatedOidAid;
    
    /**
     * 유사도 점수
     */
    private Float similarity;
    
    /**
     * 카테고리
     */
    private String category;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
}
