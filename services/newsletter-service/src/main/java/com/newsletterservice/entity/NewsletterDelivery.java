package com.newsletterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "newsletter_delivery", indexes = {
    
    // 가장 자주 사용되는 조회 패턴들
    @Index(name = "idx_user_id_created_at", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_newsletter_id_created_at", columnList = "newsletter_id, created_at DESC"),
    @Index(name = "idx_status_created_at", columnList = "status, created_at"),
    @Index(name = "idx_delivery_method_created_at", columnList = "delivery_method, created_at"),
    
    // 시간 기반 조회 최적화
    @Index(name = "idx_sent_at", columnList = "sent_at"),
    @Index(name = "idx_opened_at", columnList = "opened_at"),
    @Index(name = "idx_scheduled_at", columnList = "scheduled_at"),
    
    // 복합 인덱스 - 자주 함께 사용되는 컬럼들
    @Index(name = "idx_user_status", columnList = "user_id, status"),
    @Index(name = "idx_newsletter_status", columnList = "newsletter_id, status"),
    @Index(name = "idx_status_method", columnList = "status, delivery_method"),
    
    // 통계 쿼리 최적화
    @Index(name = "idx_created_at_status", columnList = "created_at, status"),
    @Index(name = "idx_sent_at_opened_at", columnList = "sent_at, opened_at"),
    
    // 파티셔닝을 위한 인덱스 (날짜 기반)
    @Index(name = "idx_partition_date", columnList = "created_at, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "newsletter_id", nullable = false)
    private Long newsletterId;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt; // 열람 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "delivery_method")
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod; // EMAIL, PUSH, SMS

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt; // 예약 발송 시간

    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer retryCount = 0; // 재시도 횟수

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 에러 메시지

    @Column(name = "personalized_content", columnDefinition = "TEXT")
    private String personalizedContent; // 개인화된 콘텐츠

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 상태 업데이트 메서드
    public void updateStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
    }

    // 재시도 횟수 증가 메서드
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }
    
    // 성공적인 발송 여부 확인
    public boolean isSuccessful() {
        return status == DeliveryStatus.SENT || status == DeliveryStatus.OPENED;
    }
    
    // 실패한 발송 여부 확인
    public boolean isFailed() {
        return status == DeliveryStatus.FAILED || status == DeliveryStatus.BOUNCED;
    }
    
    // 열람 여부 확인
    public boolean isOpened() {
        return openedAt != null;
    }
    
    // 발송 완료 여부 확인
    public boolean isSent() {
        return sentAt != null;
    }
}