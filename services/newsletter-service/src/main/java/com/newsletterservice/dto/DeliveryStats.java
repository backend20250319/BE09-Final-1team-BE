package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStats {
    private Long deliveryId;
    private int totalRecipients;
    private int deliveredCount;
    private int failedCount;
    private int openedCount;
    private int clickedCount;
    private double openRate;
    private double clickRate;
    private LocalDateTime deliveryTime;
    private String status;
    private String errorMessage;
    private int totalSent;
    private int totalFailed;
    private int totalScheduled;
    private double successRate;
}
