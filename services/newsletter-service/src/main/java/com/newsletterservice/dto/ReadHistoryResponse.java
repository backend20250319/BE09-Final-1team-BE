package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 읽기 기록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadHistoryResponse {
    
    private Long id;
    private Long userId;
    private Long newsId;
    private String newsTitle;
    private String categoryName;
    private LocalDateTime readAt;
    private String source; // EMAIL_NEWSLETTER, WEB, MOBILE 등
}
