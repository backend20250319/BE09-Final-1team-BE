package com.newsletterservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEngagement {
    private Long userId;
    private long totalReceived;
    private long totalOpened;
    private double engagementRate;
    private Double avgOpenDelayMinutes;
    private String recommendation;
    private int analysisPeriod;
}
