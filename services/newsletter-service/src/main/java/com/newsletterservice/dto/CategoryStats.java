package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStats {
    private Map<String, Integer> categoryCounts;
    private String topCategory;
    private int totalArticles;
    private double categoryDiversity;
}
