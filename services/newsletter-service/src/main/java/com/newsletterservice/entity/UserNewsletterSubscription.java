package com.newsletterservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자별 뉴스레터 구독 정보를 나타내는 엔티티
 */
@Entity
@Table(name = "user_newsletter_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNewsletterSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private LocalDateTime subscribedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "frequency", length = 20)
    private String frequency;

    @Column(name = "send_time", length = 10)
    private String sendTime;

    @Column(name = "is_personalized")
    @Builder.Default
    private Boolean isPersonalized = false;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    /**
     * 구독이 활성 상태인지 확인
     */
    public boolean isActiveSubscription() {
        return isActive != null && isActive;
    }

    /**
     * 구독 상태 토글
     */
    public void toggleSubscription() {
        this.isActive = !this.isActive;
    }

    /**
     * 구독 활성화
     */
    public void activateSubscription() {
        this.isActive = true;
    }

    /**
     * 구독 비활성화
     */
    public void deactivateSubscription() {
        this.isActive = false;
    }
}
