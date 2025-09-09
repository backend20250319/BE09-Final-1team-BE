package com.newsletterservice.dto;

import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.entity.NewsCategory;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private String email;
    private SubscriptionStatus status;
    private SubscriptionFrequency frequency;
    private List<NewsCategory> preferredCategories;
    private List<String> keywords;
    private Integer sendTime;
    private boolean isPersonalized;
    private LocalDateTime subscribedAt;
    private LocalDateTime lastSentAt;
    private LocalDateTime createdAt;
}
