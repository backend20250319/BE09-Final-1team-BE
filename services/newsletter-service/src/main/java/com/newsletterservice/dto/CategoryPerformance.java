package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformance {
    private String category;
    private double engagementRate;
    private int deliveryCount;
    private int openCount;
    private int clickCount;
    private double openRate;
    private double clickRate;
}
