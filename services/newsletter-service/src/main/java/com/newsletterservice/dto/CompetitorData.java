package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorData {
    private String competitorName;
    private double openRate;
    private double clickRate;
    private int subscriberCount;
    private String contentFocus;
    private String frequency;
    private double marketShare;
}
