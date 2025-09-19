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
public class NewsSearchRequest {
    private String keyword;
    private String category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> tags;
    private Integer limit;
}