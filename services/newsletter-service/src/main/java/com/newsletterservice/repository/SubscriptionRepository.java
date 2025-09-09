package com.newsletterservice.repository;

import com.newsletterservice.entity.Subscription;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.entity.SubscriptionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {


    // 사용자별 구독 정보 조회
    Optional<Subscription> findByUserId(Long userId);
    
    // 사용자별 구독 목록 조회
    List<Subscription> findAllByUserId(Long userId);
    
    // 사용자별 상태별 구독 조회
    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    // 구독 상태별 조회
    List<Subscription> findByStatus(SubscriptionStatus status);

    // 발송 가능한 구독자들 조회
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE'")
    List<Subscription> findActiveSubscriptions();

    // 상태별 구독자 수 통계
    @Query("SELECT s.status, COUNT(s) FROM Subscription s GROUP BY s.status")
    List<Object[]> getSubscriptionStatusStats();

    // 특정 사용자의 활성 구독 확인
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    List<Subscription> findActiveSubscriptionsByUser(@Param("userId") Long userId);


    // 발송 주기별 활성 구독자 조회
    List<Subscription> findByStatusAndFrequency(SubscriptionStatus status, SubscriptionFrequency frequency);

    // 이메일로 구독 정보 조회
    Optional<Subscription> findByEmail(String email);


    // 발송 대상자 조회 (일일 발송)
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.frequency = 'DAILY'
        AND (s.lastSentAt IS NULL OR s.lastSentAt < :cutoffTime)
        AND (:sendTime IS NULL OR s.sendTime = :sendTime)
    """)
    List<Subscription> findDailyDeliveryTargets(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("sendTime") Integer sendTime
    );

    // 발송 대상자 조회 (주간 발송) - 더 많은 콘텐츠 포함
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.frequency = 'WEEKLY'
        AND (s.lastSentAt IS NULL OR s.lastSentAt < :cutoffTime)
    """)
    List<Subscription> findWeeklyDeliveryTargets(@Param("cutoffTime") LocalDateTime cutoffTime);

    // 월간 발송 대상자 조회 - 심층적인 분석 리포트 발송
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE'
        AND s.frequency = 'MONTHLY'  
        AND (s.lastSentAt IS NULL OR s.lastSentAt < :cutoffTime)
        ORDER BY s.subscribedAt ASC
    """)
    List<Subscription> findMonthlyDeliveryTargets(@Param("cutoffTime") LocalDateTime cutoffTime);


    // 구독자 통계
    @Query("SELECT s.frequency, COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' GROUP BY s.frequency")
    List<Object[]> getActiveSubscriptionStats();
    
    // ========================================
    // 추가 필요한 메서드들
    // ========================================
    
    /**
     * 특정 카테고리를 포함하는 구독자 수 조회
     */
    long countByPreferredCategoriesContaining(String category);
    
    /**
     * 특정 카테고리를 포함하고 특정 상태인 구독자 수 조회
     */
    long countByPreferredCategoriesContainingAndStatus(String category, SubscriptionStatus status);
    
    /**
     * 특정 카테고리를 포함하고 특정 시간 이전에 생성된 구독자 수 조회
     */
    long countByPreferredCategoriesContainingAndCreatedAtBefore(String category, LocalDateTime date);
    
    /**
     * 특정 카테고리를 포함하고 특정 상태이고 특정 시간 이후에 업데이트된 구독자 수 조회
     */
    long countByPreferredCategoriesContainingAndStatusAndUpdatedAtAfter(String category, SubscriptionStatus status, LocalDateTime date);
    
    /**
     * 특정 카테고리를 포함하고 특정 상태인 구독자 목록 조회
     */
    List<Subscription> findByPreferredCategoriesContainingAndStatus(String category, SubscriptionStatus status);
}