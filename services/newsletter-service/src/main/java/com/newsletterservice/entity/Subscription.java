package com.newsletterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "subscription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId; // user-service 참조

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private SubscriptionFrequency frequency;

    // 관심 카테고리 (JSON 형태로 저장)
    @Column(name = "preferred_categories", columnDefinition = "TEXT")
    private String preferredCategories; // JSON: ["POLITICS", "ECONOMY", "SPORTS"]

    // 관심 키워드
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords; // JSON: ["AI", "스타트업", "투자"]

    @Column(name = "email", nullable = false)
    private String email; // 발송 이메일

    @Column(name = "send_time")
    private Integer sendTime; // 발송 시간 (24시간 기준, 9 = 오전 9시)

    @Column(name = "is_personalized")
    private boolean isPersonalized; // 개인화 뉴스레터 여부

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt; // 마지막 발송 시간

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}