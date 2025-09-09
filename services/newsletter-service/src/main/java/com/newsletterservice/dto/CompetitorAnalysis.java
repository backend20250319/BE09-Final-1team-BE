package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorAnalysis {
    private String newsletterType;
    private double myOpenRate;
    private double industryAverageOpenRate;
    private double myClickRate;
    private double industryAverageClickRate;
    private List<CompetitorData> topCompetitors;
    private String marketPosition;
    private List<String> improvementSuggestions;
    private double performanceScore;
    private double industryAverage;
    private double percentileRank;
    private String performanceLevel;
    private String recommendation;
}
