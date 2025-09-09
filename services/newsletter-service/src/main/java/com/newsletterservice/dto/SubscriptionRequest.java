package com.newsletterservice.dto;

import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.entity.NewsCategory;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    private Long userId;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String email;

    @NotNull(message = "구독 주기는 필수입니다")
    private SubscriptionFrequency frequency;

    private List<NewsCategory> preferredCategories;

    private List<String> keywords;

    @Min(value = 0, message = "발송 시간은 0-23 사이여야 합니다")
    @Max(value = 23, message = "발송 시간은 0-23 사이여야 합니다")
    private Integer sendTime; // 기본값: 9 (오전 9시)

    @Builder.Default
    private boolean isPersonalized = true; // 기본값: 개인화 활성
}
