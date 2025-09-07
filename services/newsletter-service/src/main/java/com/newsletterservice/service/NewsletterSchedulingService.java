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

}