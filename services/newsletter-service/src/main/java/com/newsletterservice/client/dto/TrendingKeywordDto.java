package com.newsletterservice.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingKeywordDto {
    private String keyword;
    private Long count;
    private Double trendScore;
}
