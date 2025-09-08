package com.newsletterservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterSchedulingService {

    private final NewsletterDeliveryService deliveryService;
    private final NewsletterAnalyticsService analyticsService;
    private final UnifiedNewsletterScheduler unifiedScheduler;

    /**
     * 카테고리별 구독자 수 동기화 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void syncCategorySubscriberCounts() {
        try {
            log.info("카테고리별 구독자 수 동기화 시작");
            analyticsService.syncCategorySubscriberCounts();
        } catch (Exception e) {
            log.error("카테고리별 구독자 수 동기화 중 오류 발생", e);
        }
    }

    /**
     * 뉴스레터 발송 통계 동기화 (매일 새벽 1시)
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void syncNewsletterDeliveryStats() {
        try {
            log.info("뉴스레터 발송 통계 동기화 시작");
            analyticsService.syncNewsletterDeliveryStats();
        } catch (Exception e) {
            log.error("뉴스레터 발송 통계 동기화 중 오류 발생", e);
        }
    }

    /**
     * 수동 뉴스레터 발송 (관리자용)
     */
    public void sendManualNewsletter(String frequency, java.util.List<Long> userIds) {
        try {
            log.info("수동 뉴스레터 발송 시작 - frequency: {}, userIds: {}", frequency, userIds);
            unifiedScheduler.sendManualNewsletter(frequency, userIds);
        } catch (Exception e) {
            log.error("수동 뉴스레터 발송 실패", e);
            throw new RuntimeException("수동 뉴스레터 발송 실패", e);
        }
    }

}