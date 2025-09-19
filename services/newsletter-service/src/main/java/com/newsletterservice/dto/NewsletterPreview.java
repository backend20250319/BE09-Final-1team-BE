package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterPreview {
    private Long userId;
    private String title;
    private String htmlContent;
    private int articleCount;
    private LocalDateTime generatedAt;
}
