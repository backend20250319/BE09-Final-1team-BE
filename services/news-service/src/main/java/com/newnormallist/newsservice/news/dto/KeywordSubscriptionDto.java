package com.newnormallist.newsservice.news.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordSubscriptionDto {
    
    private Long subscriptionId;
    private Long userId;
    private String keyword;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
