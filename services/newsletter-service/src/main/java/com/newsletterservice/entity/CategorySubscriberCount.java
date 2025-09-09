package com.newsletterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "category_subscriber_count")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySubscriberCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, unique = true)
    private NewsCategory category;

    @Column(name = "active_subscriber_count", nullable = false)
    @Builder.Default
    private Long activeSubscriberCount = 0L;

    @Column(name = "total_subscriber_count", nullable = false)
    @Builder.Default
    private Long totalSubscriberCount = 0L;

    @Column(name = "last_updated", nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 활성 구독자 수 증가
     */
    public void incrementActiveCount() {
        this.activeSubscriberCount++;
        this.totalSubscriberCount++;
    }

    /**
     * 활성 구독자 수 감소
     */
    public void decrementActiveCount() {
        if (this.activeSubscriberCount > 0) {
            this.activeSubscriberCount--;
        }
    }

    /**
     * 총 구독자 수 증가 (비활성 구독 포함)
     */
    public void incrementTotalCount() {
        this.totalSubscriberCount++;
    }

    /**
     * 총 구독자 수 감소 (비활성 구독 포함)
     */
    public void decrementTotalCount() {
        if (this.totalSubscriberCount > 0) {
            this.totalSubscriberCount--;
        }
    }
}
