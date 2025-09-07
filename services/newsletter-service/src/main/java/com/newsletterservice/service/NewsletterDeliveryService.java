package com.newsletterservice.service;

import com.newsletterservice.dto.DeliveryStats;
import com.newsletterservice.dto.NewsletterDeliveryRequest;
import com.newsletterservice.entity.NewsletterDelivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 뉴스레터 발송 전용 서비스 인터페이스
 */
public interface NewsletterDeliveryService {
    
    /**
     * 뉴스레터 즉시 발송
     */
    DeliveryStats sendNewsletterNow(NewsletterDeliveryRequest request, Long senderId);
    
    /**
     * 뉴스레터 예약 발송
     */
    DeliveryStats scheduleNewsletter(NewsletterDeliveryRequest request, Long userId);
    
    /**
     * 발송 취소
     */
    void cancelDelivery(Long deliveryId, Long userId);
    
    /**
     * 발송 재시도
     */
    void retryDelivery(Long deliveryId, Long userId);
    
    /**
     * 사용자별 발송 기록 조회
     */
    Page<NewsletterDelivery> getDeliveriesByUser(Long userId, Pageable pageable);
    
}