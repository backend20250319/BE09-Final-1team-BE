package com.newsletterservice.repository;

import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.DeliveryMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterDeliveryRepository extends JpaRepository<NewsletterDelivery, Long> {

    // ========================================
    // 1. 기본 조회 메서드들
    // ========================================
    
    /**
     * 사용자별 발송 기록 조회 (최신순)
     * 
     * 사용 목적:
     * - 사용자 마이페이지에서 "받은 뉴스레터" 목록 표시
     * - 사용자별 참여도 분석
     * - 개인 맞춤 추천 시스템 데이터
     */
    Page<NewsletterDelivery> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 뉴스레터별 발송 기록 조회
     * 
     * 사용 목적:
     * - 특정 뉴스레터의 전체 발송 현황 모니터링
     * - 관리자 대시보드에서 뉴스레터별 성과 분석
     */
    List<NewsletterDelivery> findByNewsletterIdOrderByCreatedAtDesc(Long newsletterId);
    
    /**
     * 발송 상태별 조회
     * 
     * 사용 목적:
     * - 실시간 발송 모니터링
     * - 시스템 헬스 체크
     */
    Page<NewsletterDelivery> findByStatus(DeliveryStatus status, Pageable pageable);
    
    /**
     * 발송 방법별 조회
     * 
     * 사용 목적:
     * - 채널별 성과 분석
     * - 비용 분석 (SMS는 비용 발생)
     */
    List<NewsletterDelivery> findByDeliveryMethod(DeliveryMethod method);
    
    // ========================================
    // 2. 복합 조건 조회 메서드들
    // ========================================
    
    /**
     * 사용자별 + 상태별 조회
     * 
     * 사용 목적:
     * - "김철수님이 읽지 않은 뉴스레터는?"
     * - "이 사용자의 발송 실패 건들은?"
     */
    List<NewsletterDelivery> findByUserIdAndStatus(Long userId, DeliveryStatus status);
    
    /**
     * 뉴스레터별 + 상태별 조회
     * 
     * 사용 목적:
     * - "오늘 발송한 뉴스레터 중 성공한 건수는?"
     * - 특정 뉴스레터의 발송 품질 체크
     */
    List<NewsletterDelivery> findByNewsletterIdAndStatus(Long newsletterId, DeliveryStatus status);
    
    /**
     * 기간별 + 상태별 조회
     * 
     * 사용 목적:
     * - "지난주 발송 성공률은?"
     * - 시계열 분석 데이터
     */
    List<NewsletterDelivery> findByStatusAndCreatedAtBetween(
        DeliveryStatus status, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    // ========================================
    // 3. 시간 기반 조회 메서드들
    // ========================================
    
    /**
     * 특정 기간 발송 기록 조회
     * 
     * 사용 목적:
     * - 월간/주간 리포트 생성
     * - 성과 분석 대시보드
     */
    @Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.sentAt BETWEEN :startDate AND :endDate ORDER BY nd.sentAt DESC")
    List<NewsletterDelivery> findBySentAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 최근 N일간 사용자별 발송 기록
     * 
     * 사용 목적:
     * - 사용자 참여도 계산
     * - 개인화 알고리즘 학습 데이터
     */
    @Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.userId = :userId AND nd.createdAt >= :sinceDate ORDER BY nd.createdAt DESC")
    List<NewsletterDelivery> findByUserIdAndCreatedAtAfter(
        @Param("userId") Long userId,
        @Param("sinceDate") LocalDateTime sinceDate
    );
    
    /**
     * 예약 발송 대상 조회
     * 
     * 사용 목적:
     * - 스케줄러가 발송할 뉴스레터 찾기
     * - 배치 작업 처리
     */
    @Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.status = 'PENDING' AND nd.scheduledAt <= :now")
    List<NewsletterDelivery> findPendingDeliveriesForSchedule(@Param("now") LocalDateTime now);
    
    // ========================================
    // 4. 통계 및 집계 메서드들
    // ========================================
    
    /**
     * 발송 실패 건수 조회
     * 
     * 사용 목적:
     * - 시스템 안정성 모니터링
     * - 알림 트리거 조건
     */
    @Query("SELECT COUNT(nd) FROM NewsletterDelivery nd WHERE nd.status = 'FAILED' AND nd.createdAt >= :date")
    Long countFailedDeliveriesAfter(@Param("date") LocalDateTime date);
    
    /**
     * 사용자별 열람 건수
     * 
     * 사용 목적:
     * - 사용자 참여도 측정
     * - 충성도 분석
     */
    @Query("SELECT COUNT(nd) FROM NewsletterDelivery nd WHERE nd.userId = :userId AND nd.openedAt IS NOT NULL")
    Long countOpenedByUserId(@Param("userId") Long userId);
    
    /**
     * 뉴스레터별 성과 통계 (개선된 버전)
     * 
     * 사용 목적:
     * - 콘텐츠 성과 분석
     * - 에디터 피드백 제공
     */
    @Query("""
        SELECT nd.newsletterId, 
               COUNT(nd) as totalSent, 
               SUM(CASE WHEN nd.openedAt IS NOT NULL THEN 1 ELSE 0 END) as totalOpened,
               SUM(CASE WHEN nd.status = 'FAILED' THEN 1 ELSE 0 END) as totalFailed,
               AVG(CASE WHEN nd.sentAt IS NOT NULL AND nd.createdAt IS NOT NULL 
                   THEN TIMESTAMPDIFF(SECOND, nd.createdAt, nd.sentAt) 
                   ELSE NULL END) as avgDeliveryTimeSeconds
        FROM NewsletterDelivery nd 
        WHERE nd.createdAt >= :since
        GROUP BY nd.newsletterId
        ORDER BY totalSent DESC
    """)
    List<Object[]> getNewsletterPerformanceStats(@Param("since") LocalDateTime since);
    
    /**
     * 시간대별 발송 성과 분석
     * 
     * 사용 목적:
     * - 최적 발송 시간 찾기
     * - 타임존별 최적화
     */
    @Query("""
        SELECT HOUR(nd.sentAt) as sendHour,
               COUNT(nd) as totalSent,
               SUM(CASE WHEN nd.openedAt IS NOT NULL THEN 1 ELSE 0 END) as totalOpened
        FROM NewsletterDelivery nd 
        WHERE nd.sentAt IS NOT NULL 
        AND nd.sentAt >= :since
        GROUP BY HOUR(nd.sentAt)
        ORDER BY sendHour
    """)
    List<Object[]> getHourlyPerformanceStats(@Param("since") LocalDateTime since);
    
    /**
     * 사용자별 참여도 계산
     * 
     * 사용 목적:
     * - 개인화 수준 결정
     * - 구독 빈도 최적화
     */
    @Query("""
        SELECT nd.userId,
               COUNT(nd) as totalReceived,
               SUM(CASE WHEN nd.openedAt IS NOT NULL THEN 1 ELSE 0 END) as totalOpened,
               AVG(CASE WHEN nd.openedAt IS NOT NULL AND nd.sentAt IS NOT NULL
                   THEN TIMESTAMPDIFF(MINUTE, nd.sentAt, nd.openedAt)
                   ELSE NULL END) as avgOpenDelayMinutes
        FROM NewsletterDelivery nd 
        WHERE nd.userId = :userId 
        AND nd.createdAt >= :since
        GROUP BY nd.userId
    """)
    Optional<Object[]> getUserEngagementStats(
        @Param("userId") Long userId, 
        @Param("since") LocalDateTime since
    );
    
    // ========================================
    // 5. 업데이트 및 삭제 메서드들
    // ========================================
    
    /**
     * 발송 상태 일괄 업데이트
     * 
     * 사용 목적:
     * - 배치 발송 처리
     * - 성능 최적화
     */
    @Modifying
    @Query("UPDATE NewsletterDelivery nd SET nd.status = :newStatus, nd.updatedAt = CURRENT_TIMESTAMP WHERE nd.status = :currentStatus AND nd.createdAt >= :since")
    int bulkUpdateStatus(
        @Param("currentStatus") DeliveryStatus currentStatus,
        @Param("newStatus") DeliveryStatus newStatus,
        @Param("since") LocalDateTime since
    );
    
    /**
     * 열람 시간 업데이트
     * 
     * 사용 목적:
     * - 이메일 추적 픽셀 로드 시 호출
     * - 정확한 열람 시간 기록
     */
    @Modifying
    @Query("UPDATE NewsletterDelivery nd SET nd.status = 'OPENED', nd.openedAt = :openedAt, nd.updatedAt = CURRENT_TIMESTAMP WHERE nd.id = :deliveryId AND nd.openedAt IS NULL")
    int markAsOpened(
        @Param("deliveryId") Long deliveryId, 
        @Param("openedAt") LocalDateTime openedAt
    );
    
    /**
     * 오래된 발송 기록 정리
     * 
     * 사용 목적:
     * - 데이터베이스 용량 관리
     * - 개인정보 보호
     */
    @Modifying
    @Query("DELETE FROM NewsletterDelivery nd WHERE nd.createdAt < :cutoffDate AND nd.status IN ('FAILED', 'BOUNCED')")
    int deleteOldFailedDeliveries(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // ========================================
    // 6. 고급 분석 메서드들
    // ========================================
    
    /**
     * 실시간 대시보드용 통계
     * 
     * 사용 목적:
     * - 운영팀 모니터링 대시보드
     * - 실시간 알림 시스템
     */
    @Query("""
        SELECT 
            SUM(CASE WHEN nd.status = 'PENDING' THEN 1 ELSE 0 END) as pendingCount,
            SUM(CASE WHEN nd.status = 'PROCESSING' THEN 1 ELSE 0 END) as processingCount,
            SUM(CASE WHEN nd.status = 'SENT' THEN 1 ELSE 0 END) as sentCount,
            SUM(CASE WHEN nd.status = 'OPENED' THEN 1 ELSE 0 END) as openedCount,
            SUM(CASE WHEN nd.status = 'FAILED' THEN 1 ELSE 0 END) as failedCount,
            SUM(CASE WHEN nd.status = 'BOUNCED' THEN 1 ELSE 0 END) as bouncedCount
        FROM NewsletterDelivery nd 
        WHERE nd.createdAt >= :since
    """)
    Object[] getRealTimeStats(@Param("since") LocalDateTime since);
    
    /**
     * A/B 테스트 성과 비교
     * 
     * 사용 목적:
     * - "제목 A vs 제목 B 중 어느 것이 더 효과적인가?"
     * - 데이터 기반 의사결정
     */
    @Query("""
        SELECT nd.deliveryMethod,
               nd.newsletterId,
               COUNT(nd) as totalSent,
               AVG(CASE WHEN nd.openedAt IS NOT NULL THEN 1.0 ELSE 0.0 END) as openRate,
               AVG(CASE WHEN nd.sentAt IS NOT NULL AND nd.createdAt IS NOT NULL 
                   THEN TIMESTAMPDIFF(SECOND, nd.createdAt, nd.sentAt) 
                   ELSE NULL END) as avgDeliveryTime
        FROM NewsletterDelivery nd 
        WHERE nd.newsletterId IN :newsletterIds
        AND nd.createdAt >= :since
        GROUP BY nd.deliveryMethod, nd.newsletterId
    """)
    List<Object[]> getABTestResults(
        @Param("newsletterIds") List<Long> newsletterIds,
        @Param("since") LocalDateTime since
    );
    
    // ========================================
    // 7. 기존 메서드들 (호환성 유지)
    // ========================================
    
    List<NewsletterDelivery> findByStatusAndUpdatedAtAfterAndRetryCountLessThan(DeliveryStatus deliveryStatus, LocalDateTime cutoff, int i);

    long countByStatusAndCreatedAtBetween(DeliveryStatus deliveryStatus, LocalDateTime startDate, LocalDateTime endDate);

    // 예약된 발송 목록 조회 (PENDING 상태이고 scheduledAt이 현재 시간보다 이전인 것들)
    @Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.status = :status AND nd.scheduledAt <= :now")
    List<NewsletterDelivery> findByStatusAndScheduledAtBefore(
            @Param("status") DeliveryStatus status, 
            @Param("now") LocalDateTime now);

    // 오래된 발송 기록 조회 (정리용)
    List<NewsletterDelivery> findByCreatedAtBeforeAndStatusIn(LocalDateTime cutoff, List<DeliveryStatus> statuses);
    
    // 기존 메서드 (호환성 유지)
    @Query("""
        SELECT nd.newsletterId, COUNT(nd) as totalSent, 
               SUM(CASE WHEN nd.openedAt IS NOT NULL THEN 1 ELSE 0 END) as totalOpened
        FROM NewsletterDelivery nd 
        WHERE nd.status = 'SENT' 
        GROUP BY nd.newsletterId
    """)
    List<Object[]> getNewsletterPerformanceStats();
    
    // ========================================
    // 8. 추가 필요한 메서드들
    // ========================================
    
    /**
     * 특정 시간 이후 생성된 발송 기록 조회
     */
    List<NewsletterDelivery> findByCreatedAtAfter(LocalDateTime since);
    
    /**
     * 사용자별 특정 기간 발송 기록 조회
     */
    List<NewsletterDelivery> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}