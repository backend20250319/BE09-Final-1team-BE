package com.newnormallist.newsservice.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

// 조회 로그 : category를 denormalize해서 함께 저장 (집계 성능 up)
// R(c) 계산 시 최근 7일 데이터를 감쇠 가중합 -> 비율화
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_read_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "news_id"}))
public class UserReadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name")
    private RecommendationCategory categoryName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @Builder
    public UserReadHistory(UserEntity userEntity, Long newsId, RecommendationCategory categoryName) {
        this.userEntity = userEntity;
        this.newsId = newsId;
        this.categoryName = categoryName;
    }

    public void updateReadTime() {
        this.updatedAt = LocalDateTime.now();
    }
}