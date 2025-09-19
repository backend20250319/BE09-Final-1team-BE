package com.newnormallist.newsservice.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

// 핵심 캐시 테이블 (유저 x 9행)
// score, wDemo, wPref, wRead, wScrap, updated_at 컬럼 존재
// 요청 시 여기서 상위 3개의 카테고리만 읽어 피드 조립 -> 빠른 응답 보장

@Entity
@Table(name = "user_pref_vector",
       indexes = @Index(name="idx_upv_user_score", columnList="userId, score DESC"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(UserPrefVector.PK.class)
public class UserPrefVector {

    @Id @Column(nullable=false)
    private Long userId;

    @Id @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private RecommendationCategory category;

    @Column(nullable=false)
    private double score;   // normalized 0~1

    @Column(nullable=false)
    private double wDemo;

    @Column(nullable=false)
    private double wPref;

    @Column(nullable=false)
    private double wRead;

    @Column(nullable=false)
    private double wScrap;

    @CreationTimestamp
    @Column(nullable=false, updatable=false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable=false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @Data
    public static class PK implements java.io.Serializable {
        private Long userId;
        private RecommendationCategory category;
    }
}