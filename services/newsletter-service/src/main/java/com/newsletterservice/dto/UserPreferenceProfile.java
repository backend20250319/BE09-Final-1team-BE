package com.newsletterservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceProfile {
    private Long userId;
    private List<String> preferredCategories;
    private Integer recentInteractions;
    private Integer mostActiveHour;
    private Integer preferredReadTime;
    private Double engagementScore;
    private LocalDateTime lastAnalyzed;
}
