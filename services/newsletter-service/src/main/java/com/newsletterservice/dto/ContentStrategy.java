package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStrategy {
    private String strategyType;
    private List<String> preferredCategories;
    private Map<String, Double> categoryWeights;
    private int maxArticlesPerCategory;
    private boolean includeTrending;
    private boolean includePersonalized;
    private String contentMix;
    private double diversityFactor;
    private int maxArticles;
    private String preferredLength;
    private boolean prioritizeRecent;
    private boolean includeBreaking;
    private boolean includeAnalysis;
    private boolean includeExpert;
    private String tone;
}
