package com.newsletterservice.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedUserEngagement {
    private UserEngagement basicEngagement;
    private Map<String, Long> categoryInteractions;
    private Map<String, Long> interactionTypeDistribution;
    private long totalInteractions;
    private int mostActiveHour;
    private List<Double> engagementTrend;
    private Map<String, Double> averageReadingTimeByCategory;
}
