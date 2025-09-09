package com.newnormallist.userservice.history.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestResponse {
    private Long userId;
    private List<String> topInterests;
    private Map<String, Double> interestScores;
    private String analysisSummary;
}
