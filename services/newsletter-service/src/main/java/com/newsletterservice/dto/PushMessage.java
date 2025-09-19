package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 웹 푸시 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {
    private String title;
    private String body;
    private String url;
    private String icon;
    private String badge;
}
