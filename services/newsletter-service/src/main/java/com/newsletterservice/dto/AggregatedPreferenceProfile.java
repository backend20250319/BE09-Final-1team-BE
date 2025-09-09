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
public class AggregatedPreferenceProfile {
    private List<Long> userIds;
    private Map<String, Double> categoryPreferences;
    private Map<String, Double> keywordPreferences;
    private List<String> preferredTopics;
    private double averageEngagementRate;
    private String preferredContentType;
    private Map<String, Integer> readHistory;
    private List<String> trendingTopics;
    private int totalUsers;
    private List<String> dominantCategories;
    private Map<String, Integer> categoryFrequency;
    private double diversityScore;
    private LocalDateTime analyzedAt;
}
