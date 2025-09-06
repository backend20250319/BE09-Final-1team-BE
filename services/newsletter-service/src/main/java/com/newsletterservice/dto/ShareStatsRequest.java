package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareStatsRequest {
    private String type; // 'kakao', 'facebook', 'twitter' 등
    private Long newsId; // 선택적: 특정 뉴스 ID
    private String category; // 선택적: 특정 카테고리
}
