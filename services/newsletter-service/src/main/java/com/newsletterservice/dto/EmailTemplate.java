package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 템플릿 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    private String subject;
    private String htmlContent;
    private String textContent;
}