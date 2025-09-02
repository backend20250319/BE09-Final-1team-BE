package com.newnormallist.newsservice.summarizer.dto;

import com.newnormallist.newsservice.summarizer.entity.NewsSummaryEntity;
import lombok.*;

import java.time.Instant;

@Builder
public record SummaryResponse(
        Long id,
        Long newsId,
        String type,
        int lines,
        String summary,
        boolean cached,
        Instant createdAt
) {
    public static SummaryResponse fromEntity(NewsSummaryEntity e, boolean cached) {
        return SummaryResponse.builder()
                .id(e.getId())
                .newsId(e.getNewsId())
                .type(e.getSummaryType())
                .lines(e.getLines())
                .summary(e.getSummaryText())
                .cached(cached)
                .createdAt(e.getCreatedAt())
                .build();
    }
}