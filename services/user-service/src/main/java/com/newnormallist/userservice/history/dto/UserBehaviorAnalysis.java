package com.newnormallist.userservice.history.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

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
