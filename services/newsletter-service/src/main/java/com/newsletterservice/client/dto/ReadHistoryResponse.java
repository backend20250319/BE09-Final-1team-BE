package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Newsletter 서비스에서 사용하는 읽기 이력 응답 DTO
 * User Service의 ReadHistoryResponse와 호환되도록 설계
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
