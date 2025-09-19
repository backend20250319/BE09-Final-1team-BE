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
public class UserPreferenceProfile {
    private Long userId;
    private Map<String, Double> categoryPreferences;
    private List<String> preferredKeywords;
    private String preferredContentType;
    private double engagementRate;
}
