package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurationResult {
    private List<NewsletterContent.Article> articles;
    private int totalArticles;
    private String curationStrategy;
    private double averageRelevanceScore;
}
