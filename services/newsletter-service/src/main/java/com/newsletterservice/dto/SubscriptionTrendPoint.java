package com.newsletterservice.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTrendPoint {
    private LocalDate date;
    private Long subscriberCount;
    private Long dailyChange;
}
