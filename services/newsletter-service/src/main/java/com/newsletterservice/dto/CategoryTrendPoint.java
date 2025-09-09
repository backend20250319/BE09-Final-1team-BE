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
public class CategoryTrendPoint {
    private LocalDateTime date;
    private double openRate;
    private double clickRate;
    private int deliveryCount;
    private int openCount;
    private int clickCount;
}
