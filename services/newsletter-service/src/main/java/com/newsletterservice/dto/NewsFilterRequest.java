package com.newsletterservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsFilterRequest {
    private List<String> categories;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> keywords;
    private Integer limit;
    private String sortBy;
    private String sortOrder;
}
