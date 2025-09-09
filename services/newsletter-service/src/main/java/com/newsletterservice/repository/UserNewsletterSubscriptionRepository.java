package com.newsletterservice.repository;

import com.newsletterservice.entity.UserNewsletterSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자별 뉴스레터 구독 정보 Repository
 */
@Repository
public interface UserNewsletterSubscriptionRepository extends JpaRepository<UserNewsletterSubscription, Long> {

    /**
     * 사용자 ID로 모든 구독 정보 조회
     */
    List<UserNewsletterSubscription> findByUserId(Long userId);

    /**
     * 사용자 ID와 카테고리로 구독 정보 조회 (첫 번째 구독만)
     */
    Optional<UserNewsletterSubscription> findByUserIdAndCategory(Long userId, String category);

    /**
     * 사용자 ID와 카테고리로 모든 구독 정보 조회 (다중 구독 지원)
     */
    List<UserNewsletterSubscription> findAllByUserIdAndCategory(Long userId, String category);

    /**
     * 사용자 ID로 활성 구독 정보만 조회
     */
    @Query("SELECT s FROM UserNewsletterSubscription s WHERE s.userId = :userId AND s.isActive = true")
    List<UserNewsletterSubscription> findActiveSubscriptionsByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID와 카테고리로 활성 구독 정보 조회 (첫 번째 구독만)
     */
    @Query("SELECT s FROM UserNewsletterSubscription s WHERE s.userId = :userId AND s.category = :category AND s.isActive = true")
    Optional<UserNewsletterSubscription> findActiveSubscriptionByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);

    /**
     * 사용자 ID와 카테고리로 모든 활성 구독 정보 조회 (다중 구독 지원)
     */
    @Query("SELECT s FROM UserNewsletterSubscription s WHERE s.userId = :userId AND s.category = :category AND s.isActive = true")
    List<UserNewsletterSubscription> findAllActiveSubscriptionsByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);

    /**
     * 구독 상태 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserNewsletterSubscription s SET s.isActive = :isActive, s.updatedAt = CURRENT_TIMESTAMP WHERE s.userId = :userId AND s.category = :category")
    int updateSubscriptionStatus(@Param("userId") Long userId, @Param("category") String category, @Param("isActive") Boolean isActive);

    /**
     * 구독 ID로 상태 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserNewsletterSubscription s SET s.isActive = :isActive, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :subscriptionId AND s.userId = :userId")
    int updateSubscriptionStatusById(@Param("subscriptionId") Long subscriptionId, @Param("userId") Long userId, @Param("isActive") Boolean isActive);

    /**
     * 구독 정보 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserNewsletterSubscription s WHERE s.userId = :userId AND s.category = :category")
    int deleteByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);

    /**
     * 사용자의 모든 구독 정보 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserNewsletterSubscription s WHERE s.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 카테고리별 활성 구독자 수 조회
     */
    @Query("SELECT COUNT(s) FROM UserNewsletterSubscription s WHERE s.category = :category AND s.isActive = true")
    long countActiveSubscribersByCategory(@Param("category") String category);

    /**
     * 카테고리별 구독자 수 집계 (모든 카테고리)
     */
    @Query("SELECT s.category, COUNT(s) FROM UserNewsletterSubscription s WHERE s.isActive = true GROUP BY s.category")
    List<Object[]> countActiveSubscribersByCategory();

    /**
     * 전체 활성 구독자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT s.userId) FROM UserNewsletterSubscription s WHERE s.isActive = true")
    long countActiveSubscribers();
    
    /**
     * 전체 활성 구독자 수 조회 (Long 반환)
     */
    @Query("SELECT COUNT(DISTINCT s.userId) FROM UserNewsletterSubscription s WHERE s.isActive = true")
    Long countTotalActiveSubscribers();

    /**
     * 사용자 ID 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 사용자 ID와 카테고리 조합 존재 여부 확인
     */
    boolean existsByUserIdAndCategory(Long userId, String category);
    
    /**
     * 스케줄링용 활성 구독자 조회 (발송 시간 및 빈도 고려)
     */
    @Query("""
        SELECT s FROM UserNewsletterSubscription s 
        WHERE s.isActive = true 
        AND s.frequency = :frequency
        AND s.sendTime = :sendTime
        """)
    List<UserNewsletterSubscription> findActiveSubscriptionsForScheduling(
        @Param("frequency") String frequency,
        @Param("sendTime") String sendTime,
        @Param("now") LocalDateTime now
    );
    
    /**
     * 특정 시간대의 활성 구독자 조회 (빈도 무관)
     */
    @Query("""
        SELECT s FROM UserNewsletterSubscription s 
        WHERE s.isActive = true 
        AND s.sendTime = :sendTime
        """)
    List<UserNewsletterSubscription> findActiveSubscriptionsBySendTime(
        @Param("sendTime") String sendTime
    );
    
    /**
     * 특정 빈도의 활성 구독자 조회 (시간 무관)
     */
    @Query("""
        SELECT s FROM UserNewsletterSubscription s 
        WHERE s.isActive = true 
        AND s.frequency = :frequency
        """)
    List<UserNewsletterSubscription> findActiveSubscriptionsByFrequency(
        @Param("frequency") String frequency
    );
    
    /**
     * 개인화 설정이 활성화된 구독자 조회
     */
    @Query("""
        SELECT s FROM UserNewsletterSubscription s 
        WHERE s.isActive = true 
        AND s.isPersonalized = true
        """)
    List<UserNewsletterSubscription> findPersonalizedActiveSubscriptions();
    
    /**
     * 키워드가 설정된 구독자 조회
     */
    @Query("""
        SELECT s FROM UserNewsletterSubscription s 
        WHERE s.isActive = true 
        AND s.keywords IS NOT NULL 
        AND s.keywords != ''
        """)
    List<UserNewsletterSubscription> findSubscriptionsWithKeywords();
}
