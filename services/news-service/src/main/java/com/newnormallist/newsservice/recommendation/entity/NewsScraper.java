package com.newnormallist.newsservice.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

// 스크랩 기록. category denormalize 하는 데 쓰임
// S(c) 계산 시 최근 30일 데이터를 감쇠 가중합 -> 비율화
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "news_scrap", indexes = {
    @Index(name="idx_ns_user_time", columnList="storageId, createdAt DESC")
})
public class NewsScraper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Integer scrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private NewsEntity newsEntity;

    @Column(name = "storage_id", nullable = false)
    private Integer storageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;
}
