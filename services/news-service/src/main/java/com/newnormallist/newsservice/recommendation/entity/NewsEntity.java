package com.newnormallist.newsservice.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


// 뉴스 마스터 테이블 매핑
// 핵심 인덱스 : (category, published_at DESC) -> 카테고리별 최신 기사 추출\
@Entity
@Table(name = "news", indexes = {
    @Index(name = "idx_news_cat_pub", columnList = "category_name, published_at")
})
// WHERE category_name = ? ORDER BY published_at DESC 같은 쿼리에서 효율적으로 최신 기사를 뽑을 수 있음
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private RecommendationCategory categoryName;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "press", nullable = false, columnDefinition = "TEXT")
    private String press;

    @Column(name = "published_at", length = 100)
    private String publishedAt;

    @Column(name = "reporter", nullable = false, columnDefinition = "TEXT")
    private String reporter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_state", nullable = false)
    private DedupState dedupState;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "trusted", nullable = false)
    @Builder.Default
    private Boolean trusted = true;
    
    @Column(name = "link", nullable = false, columnDefinition = "TEXT")
    private String link;

    @Column(name = "oid_aid")
    private String oidAid;
}
