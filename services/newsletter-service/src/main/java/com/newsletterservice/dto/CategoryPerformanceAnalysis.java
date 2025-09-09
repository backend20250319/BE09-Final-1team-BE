package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceAnalysis {
    private int analysisPeriod;
    private List<CategoryPerformance> categoryPerformances;
    private String topPerformingCategory;
    private double averageEngagementRate;
    private LocalDateTime analysisDate;
}
