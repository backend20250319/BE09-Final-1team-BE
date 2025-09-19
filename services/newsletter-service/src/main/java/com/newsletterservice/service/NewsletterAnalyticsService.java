package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.dto.ShareStatsRequest;
import com.newsletterservice.dto.ShareStatsResponse;
import com.newsletterservice.dto.UserEngagement;

import java.util.List;
import java.util.Map;

/**
 * 분석 및 통계 전용 서비스 인터페이스
 */
public interface NewsletterAnalyticsService {
    
    /**
     * 개인화된 추천 기사 조회
     */
    List<NewsletterContent.Article> getPersonalizedRecommendations(Long userId, int limit);
    
    /**
     * 사용자 참여도 분석
     */
    UserEngagement analyzeUserEngagement(Long userId, int days);
    
    /**
     * 공유 통계 기록
     */
    ShareStatsResponse recordShareStats(ShareStatsRequest request, String userId);
    
    /**
     * 카테고리별 구독자 통계 조회
     */
    Map<String, Object> getCategorySubscriberStats(String category);
    
    /**
     * 전체 카테고리별 구독자 통계 조회
     */
    Map<String, Object> getAllCategoriesSubscriberStats();
    
    /**
     * 카테고리별 구독자 수 동기화
     */
    void syncCategorySubscriberCounts();
    
    /**
     * 뉴스레터 발송 통계 기록
     */
    void recordNewsletterDelivery(Long userId, String newsletterType, boolean success);
    
    /**
     * 뉴스레터 발송 통계 동기화
     */
    void syncNewsletterDeliveryStats();
}
