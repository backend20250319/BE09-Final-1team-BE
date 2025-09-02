package com.newnormallist.newsservice.summarizer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "news_summary")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsSummaryEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "summary_type", length = 50, nullable = false)
    private String summaryType; // 예: DEFAULT, POLITICS ...

    @Column(name = "lines", nullable = false)
    private Integer lines;

    @Lob
    @Column(name = "summary_text", nullable = false)
    private String summaryText; // “줄바꿈 기준 N줄 텍스트”

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
