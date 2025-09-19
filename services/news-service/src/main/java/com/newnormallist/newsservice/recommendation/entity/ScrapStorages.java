package com.newnormallist.newsservice.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

// 스크랩 저장소. category denormalize 하는 데 쓰임
// S(c) 계산 시 최근 30일 데이터를 감쇠 가중합 -> 비율화
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scrap_storage", indexes = @Index(name="idx_sb_user", columnList="userId"))
public class ScrapStorages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_id")
    private Integer storageId;

    @Column(name = "storage_name")
    private String storageName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;
}
