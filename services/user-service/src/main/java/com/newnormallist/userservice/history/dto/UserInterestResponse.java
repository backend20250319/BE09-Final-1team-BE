package com.newnormallist.userservice.history.dto;

import com.newnormallist.userservice.user.entity.NewsCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class UserInterestResponse {
    private final Long userId;
    private final List<NewsCategory> topInterests;
    private final Map<NewsCategory, Double> interestScores;
    private final String interestSummary;
}
