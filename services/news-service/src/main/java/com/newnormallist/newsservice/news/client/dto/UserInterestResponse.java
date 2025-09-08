package com.newnormallist.newsservice.news.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 사용자 관심사 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestResponse {
    
    private Long userId;
    private List<String> topCategories;
    private List<String> topInterests;
    private List<String> signupInterests; // 가입 시 선택한 관심사
    private Map<String, Double> interestScores;
    private String analysisSummary;
}
