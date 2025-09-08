package com.newsletterservice.service;

import com.newsletterservice.entity.UserNewsletterSubscription;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;
import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 통합 뉴스레터 자동 발송 스케줄러
 * 기존 FeedBNewsletterScheduler를 대체하고 모든 뉴스레터 타입을 통합 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedNewsletterScheduler {

    private final NewsletterDeliveryService deliveryService;
    private final UserNewsletterSubscriptionRepository subscriptionRepository;
    private final EnhancedKakaoIntegrationService kakaoIntegrationService;
    private final NewsletterContentService contentService;
    private final NewsletterAnalyticsService analyticsService;

    /**
     * 매일 오전 8시 통합 뉴스레터 자동 발송
     */
    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Seoul")
    @Transactional
    public void sendDailyNewsletter() {
        log.info("일일 뉴스레터 자동 발송 시작 - {}", LocalDateTime.now());
        
        try {
            // 1. 활성 구독자 조회 (실제 DB에서)
            List<UserNewsletterSubscription> activeSubscriptions = 
                subscriptionRepository.findActiveSubscriptionsForScheduling("DAILY", "08:00", LocalDateTime.now());
            
            if (activeSubscriptions.isEmpty()) {
                log.info("발송할 구독자가 없습니다.");
                return;
            }
            
            // 2. 사용자별 발송 설정에 따라 분류
            Map<Long, List<UserNewsletterSubscription>> userSubscriptions = 
                groupSubscriptionsByUser(activeSubscriptions);
            
            log.info("발송 대상 사용자 수: {}", userSubscriptions.size());
            
            // 3. 멀티채널 발송 실행
            final int[] successCount = {0};
            final int[] failureCount = {0};
            
            for (Map.Entry<Long, List<UserNewsletterSubscription>> entry : userSubscriptions.entrySet()) {
                Long userId = entry.getKey();
                List<UserNewsletterSubscription> subscriptions = entry.getValue();
                
                CompletableFuture.runAsync(() -> {
                    try {
                        sendMultiChannelNewsletter(userId, subscriptions);
                        synchronized (this) {
                            successCount[0]++;
                        }
                    } catch (Exception e) {
                        log.error("사용자 {} 뉴스레터 발송 실패", userId, e);
                        synchronized (this) {
                            failureCount[0]++;
                        }
                    }
                });
            }
            
            // 4. 발송 통계 업데이트
            updateDeliveryStats(userSubscriptions.size(), successCount[0], failureCount[0]);
            
            log.info("일일 뉴스레터 자동 발송 완료 - 성공: {}, 실패: {}", successCount[0], failureCount[0]);
            
        } catch (Exception e) {
            log.error("일일 뉴스레터 자동 발송 실패", e);
        }
    }
    
    /**
     * 매주 월요일 오전 9시 주간 뉴스레터 발송
     */
    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void sendWeeklyNewsletter() {
        log.info("주간 뉴스레터 자동 발송 시작 - {}", LocalDateTime.now());
        
        try {
            List<UserNewsletterSubscription> weeklySubscriptions = 
                subscriptionRepository.findActiveSubscriptionsForScheduling("WEEKLY", "09:00", LocalDateTime.now());
            
            if (weeklySubscriptions.isEmpty()) {
                log.info("주간 발송할 구독자가 없습니다.");
                return;
            }
            
            Map<Long, List<UserNewsletterSubscription>> userSubscriptions = 
                groupSubscriptionsByUser(weeklySubscriptions);
            
            for (Map.Entry<Long, List<UserNewsletterSubscription>> entry : userSubscriptions.entrySet()) {
                Long userId = entry.getKey();
                List<UserNewsletterSubscription> subscriptions = entry.getValue();
                
                CompletableFuture.runAsync(() -> {
                    try {
                        sendMultiChannelNewsletter(userId, subscriptions);
                    } catch (Exception e) {
                        log.error("사용자 {} 주간 뉴스레터 발송 실패", userId, e);
                    }
                });
            }
            
            log.info("주간 뉴스레터 자동 발송 완료 - 대상: {}명", userSubscriptions.size());
            
        } catch (Exception e) {
            log.error("주간 뉴스레터 자동 발송 실패", e);
        }
    }
    
    /**
     * 매월 1일 오전 10시 월간 뉴스레터 발송
     */
    @Scheduled(cron = "0 0 10 1 * ?", zone = "Asia/Seoul")
    @Transactional
    public void sendMonthlyNewsletter() {
        log.info("월간 뉴스레터 자동 발송 시작 - {}", LocalDateTime.now());
        
        try {
            List<UserNewsletterSubscription> monthlySubscriptions = 
                subscriptionRepository.findActiveSubscriptionsForScheduling("MONTHLY", "10:00", LocalDateTime.now());
            
            if (monthlySubscriptions.isEmpty()) {
                log.info("월간 발송할 구독자가 없습니다.");
                return;
            }
            
            Map<Long, List<UserNewsletterSubscription>> userSubscriptions = 
                groupSubscriptionsByUser(monthlySubscriptions);
            
            for (Map.Entry<Long, List<UserNewsletterSubscription>> entry : userSubscriptions.entrySet()) {
                Long userId = entry.getKey();
                List<UserNewsletterSubscription> subscriptions = entry.getValue();
                
                CompletableFuture.runAsync(() -> {
                    try {
                        sendMultiChannelNewsletter(userId, subscriptions);
                    } catch (Exception e) {
                        log.error("사용자 {} 월간 뉴스레터 발송 실패", userId, e);
                    }
                });
            }
            
            log.info("월간 뉴스레터 자동 발송 완료 - 대상: {}명", userSubscriptions.size());
            
        } catch (Exception e) {
            log.error("월간 뉴스레터 자동 발송 실패", e);
        }
    }
    
    /**
     * 멀티채널 뉴스레터 발송 (이메일 + 카카오톡)
     */
    private void sendMultiChannelNewsletter(Long userId, List<UserNewsletterSubscription> subscriptions) {
        try {
            log.info("사용자 {} 멀티채널 뉴스레터 발송 시작 - 구독 카테고리: {}", 
                userId, subscriptions.stream().map(UserNewsletterSubscription::getCategory).collect(Collectors.toList()));
            
            // 1. 개인화된 뉴스레터 콘텐츠 생성
            // NewsletterContent content = contentService.generateContent(userId, subscriptions); // NewsletterContentService에 구현 필요
            
            // 임시 구현: 기본 콘텐츠 생성
            NewsletterContent content = new NewsletterContent();
            content.setUserId(userId);
            content.setTitle("뉴스레터 제목");
            
            // 2. 멀티채널 발송 (이메일 + 카카오톡)
            kakaoIntegrationService.sendMultiChannelNewsletter(userId, content);
            
            log.info("사용자 {} 멀티채널 뉴스레터 발송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {} 멀티채널 뉴스레터 발송 실패", userId, e);
            throw e;
        }
    }
    
    /**
     * 구독을 사용자별로 그룹화
     */
    private Map<Long, List<UserNewsletterSubscription>> groupSubscriptionsByUser(
            List<UserNewsletterSubscription> subscriptions) {
        return subscriptions.stream()
            .collect(Collectors.groupingBy(UserNewsletterSubscription::getUserId));
    }
    
    /**
     * 발송 통계 업데이트
     */
    private void updateDeliveryStats(int totalTargets, int successCount, int failureCount) {
        try {
            // 분석 서비스를 통해 발송 통계 업데이트
            log.info("발송 통계 업데이트 - 총 대상: {}, 성공: {}, 실패: {}", totalTargets, successCount, failureCount);
            // analyticsService.updateDeliveryStats(totalTargets, successCount, failureCount);
        } catch (Exception e) {
            log.error("발송 통계 업데이트 실패", e);
        }
    }
    
    /**
     * 수동 뉴스레터 발송 (관리자용)
     */
    public void sendManualNewsletter(String frequency, List<Long> userIds) {
        log.info("수동 뉴스레터 발송 시작 - frequency: {}, userIds: {}", frequency, userIds);
        
        try {
            for (Long userId : userIds) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // 사용자의 활성 구독 조회
                        List<UserNewsletterSubscription> subscriptions = 
                            subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                        
                        if (!subscriptions.isEmpty()) {
                            sendMultiChannelNewsletter(userId, subscriptions);
                        } else {
                            log.warn("사용자 {}의 활성 구독이 없습니다.", userId);
                        }
                        
                    } catch (Exception e) {
                        log.error("사용자 {} 수동 뉴스레터 발송 실패", userId, e);
                    }
                });
            }
            
            log.info("수동 뉴스레터 발송 완료 - 대상: {}명", userIds.size());
            
        } catch (Exception e) {
            log.error("수동 뉴스레터 발송 실패", e);
        }
    }
}
