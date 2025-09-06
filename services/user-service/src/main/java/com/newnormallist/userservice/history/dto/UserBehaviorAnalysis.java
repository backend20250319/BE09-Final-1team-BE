package com.newnormallist.userservice.history.dto;

import com.newnormallist.userservice.user.entity.NewsCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class UserBehaviorAnalysis {
    private final Long userId;
    private final Map<NewsCategory, Long> categoryReadCounts;
    private final Map<NewsCategory, Double> categoryPreferences;
    private final NewsCategory topCategory;
    private final Long totalReadCount;
    private final Double engagementScore;
    private final String analysisSummary;
}
